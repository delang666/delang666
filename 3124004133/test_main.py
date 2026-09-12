# -*- coding: utf-8 -*-
"""论文查重程序单元测试。

覆盖范围：
    1. SimHash 算法核心逻辑（相似度、汉明距离、自反性、对称性、增删改场景）。
    2. 文件读写与异常（UTF-8/GBK 编码、文件不存在、空文件、读写回环、OS 错误）。
    3. 命令行入口（参数校验、main() 分支覆盖、端到端输出）。

运行方式（在 paper-checker 目录下）：
    python -m unittest test_main -v
    配合覆盖率插件：
    coverage run --branch -m unittest test_main && coverage report -m
"""

import contextlib
import io
import os
import subprocess
import sys
import tempfile
import unittest
from unittest import mock

from file_handler import read_text, write_text, EmptyFileError, FileReadError
from main import check_similarity, main
from simhash import SimHash

# main.py 的绝对路径（用于端到端子进程测试）
MAIN_PY = os.path.join(os.path.dirname(os.path.abspath(__file__)), "main.py")

# 较长的基准文本，避免短文本导致 SimHash 结果不稳定
BASE_TEXT = (
    "软件工程是一门研究用工程化方法构建和维护有效的、实用的和高质量的软件的学科。"
    "它涉及程序设计语言、数据库、软件开发工具、系统平台、标准、设计模式等方面。"
    "在现代社会中，软件应用于多个方面，比如电子邮件、嵌入式系统、操作系统、数据库、游戏等。"
)
SIMILAR_TEXT = (
    "软件工程是一门研究用工程化方法构建和维护有效的、实用的和高质量的软件的科学。"
    "它涉及编程语言、数据库、软件开发工具、系统平台、标准、设计模式等方面。"
    "在现代社会中，软件被应用于多个方面，例如电子邮件、嵌入式系统、操作系统、数据库、游戏等。"
)
DIFF_TEXT = (
    "今天天气晴朗，我准备出门散步，呼吸新鲜空气，顺便去超市购买一些水果和蔬菜。"
    "回家的路上我还顺路去了一趟书店，挑选了几本喜欢的文学小说。"
)


def _write_temp(content=None, encoding="utf-8"):
    """把内容写入临时文件，返回文件路径（调用方负责删除）。

    Args:
        content: 文件内容；为 str 时按 encoding 编码，为 bytes 时原样写入，
                 None 表示空文件。
        encoding: 文本编码，默认 UTF-8。

    Returns:
        str: 临时文件绝对路径。
    """
    fd, path = tempfile.mkstemp(suffix=".txt")
    os.close(fd)
    if content is not None:
        if isinstance(content, str):
            content = content.encode(encoding)
        with open(path, "wb") as f:
            f.write(content)
    return path


class TestSimHash(unittest.TestCase):
    """SimHash 核心算法测试。"""

    def test_identical_text_similarity_is_one(self):
        """完全相同的文本，相似度应为 1.00。"""
        a = SimHash(BASE_TEXT)
        b = SimHash(BASE_TEXT)
        self.assertAlmostEqual(a.similarity(b), 1.0, places=2)

    def test_self_similarity_is_one(self):
        """自反性：任意文本与自身相似度为 1。"""
        s = SimHash(BASE_TEXT)
        self.assertEqual(s.similarity(s), 1.0)

    def test_hamming_distance_symmetric(self):
        """汉明距离具有对称性。"""
        a = SimHash(BASE_TEXT)
        b = SimHash(SIMILAR_TEXT)
        self.assertEqual(a.hamming_distance(b), b.hamming_distance(a))

    def test_similar_text_high_similarity(self):
        """仅少量改写的文本相似度应较高（> 0.6）。"""
        a = SimHash(BASE_TEXT)
        b = SimHash(SIMILAR_TEXT)
        self.assertGreater(a.similarity(b), 0.6)

    def test_completely_different_low_similarity(self):
        """主题完全不同的文本相似度应较低（< 0.8）。"""
        a = SimHash(BASE_TEXT)
        b = SimHash(DIFF_TEXT)
        self.assertLess(a.similarity(b), 0.8)

    def test_add_content_still_high_similarity(self):
        """在原文基础上追加少量内容，相似度仍应较高。"""
        added = BASE_TEXT + "此外，随着人工智能技术的快速发展，软件工程也在不断融合新的理念与工具。"
        self.assertGreater(SimHash(BASE_TEXT).similarity(SimHash(added)), 0.6)

    def test_delete_content_still_high_similarity(self):
        """删除原文一部分内容后，相似度仍应较高。"""
        deleted = BASE_TEXT[: len(BASE_TEXT) // 2]
        base_hash = SimHash(BASE_TEXT)
        self.assertGreater(base_hash.similarity(SimHash(deleted)), 0.4)

    def test_punctuation_only_is_filtered(self):
        """纯标点文本分词后应为空列表。"""
        tokens = SimHash._tokenize("，，。。！！？？")
        self.assertEqual(tokens, [])

    def test_single_char_not_crash(self):
        """单个字符不应崩溃。"""
        s = SimHash("好")
        self.assertEqual(s.similarity(s), 1.0)

    def test_empty_text_not_crash(self):
        """空文本构造不应崩溃（相似度语义由上层保证）。"""
        s = SimHash("")
        self.assertEqual(s.similarity(s), 1.0)

    def test_whitespace_token_filtered(self):
        """空白与标点 token 应被过滤。"""
        tokens = SimHash._tokenize("今天 ， 天气  \n")
        self.assertNotIn("，", tokens)


class TestFileHandler(unittest.TestCase):
    """文件读写与异常测试。"""

    def test_read_utf8(self):
        path = _write_temp(BASE_TEXT)
        try:
            self.assertEqual(read_text(path), BASE_TEXT)
        finally:
            os.remove(path)

    def test_read_gbk_fallback(self):
        content = "中文内容测试"
        path = _write_temp(content.encode("gbk"))
        try:
            self.assertEqual(read_text(path), content)
        finally:
            os.remove(path)

    def test_read_not_exists(self):
        with self.assertRaises(FileNotFoundError):
            read_text("this_path_does_not_exist_12345.txt")

    def test_write_and_read_roundtrip(self):
        path = os.path.join(tempfile.gettempdir(), "ans_roundtrip.txt")
        try:
            write_text(path, "0.85")
            self.assertEqual(read_text(path), "0.85")
        finally:
            if os.path.exists(path):
                os.remove(path)

    def test_empty_file_error(self):
        path = _write_temp()  # 空文件
        try:
            with self.assertRaises(EmptyFileError):
                check_similarity(path, path)
        finally:
            os.remove(path)

    def test_read_oserror_raises_file_read_error(self):
        """文件存在但打开失败（OSError）时应抛 FileReadError。"""
        path = _write_temp()
        try:
            with mock.patch(
                "builtins.open", side_effect=OSError("permission denied")
            ):
                with self.assertRaises(FileReadError):
                    read_text(path)
        finally:
            os.remove(path)

    def test_read_gbk_fallback_oserror(self):
        """UTF-8 解码失败且 GBK 回退也失败时应抛 FileReadError。"""
        path = _write_temp()

        def fake_open(*_args, **kwargs):
            if kwargs.get("encoding") == "utf-8":
                raise UnicodeDecodeError("utf-8", b"", 0, 1, "invalid")
            raise OSError("permission denied")

        try:
            with mock.patch("builtins.open", side_effect=fake_open):
                with self.assertRaises(FileReadError):
                    read_text(path)
        finally:
            os.remove(path)

    def test_write_error_raises_file_read_error(self):
        """写入不存在的目录时应抛 FileReadError。"""
        bad_path = os.path.join(
            tempfile.gettempdir(), "no_such_dir_xyz", "ans.txt"
        )
        with self.assertRaises(FileReadError):
            write_text(bad_path, "0.85")


class TestMainFunction(unittest.TestCase):
    """直接调用 main() 函数，覆盖各分支。"""

    def test_wrong_arg_count_exits_1(self):
        with self.assertRaises(SystemExit) as cm:
            with contextlib.redirect_stdout(io.StringIO()), \
                    contextlib.redirect_stderr(io.StringIO()):
                main(["a.txt"])
        self.assertEqual(cm.exception.code, 1)

    def test_missing_file_exits_2(self):
        with self.assertRaises(SystemExit) as cm:
            with contextlib.redirect_stdout(io.StringIO()), \
                    contextlib.redirect_stderr(io.StringIO()):
                main(["no_such.txt", "no_such2.txt", "out.txt"])
        self.assertEqual(cm.exception.code, 2)

    def test_empty_file_writes_zero(self):
        empty = _write_temp()
        ans = os.path.join(tempfile.gettempdir(), "ans_direct_empty.txt")
        try:
            with contextlib.redirect_stdout(io.StringIO()), \
                    contextlib.redirect_stderr(io.StringIO()):
                main([empty, empty, ans])
            self.assertEqual(read_text(ans).strip(), "0.00")
        finally:
            os.remove(empty)
            if os.path.exists(ans):
                os.remove(ans)

    def test_success_writes_one(self):
        orig = _write_temp(BASE_TEXT)
        ans = os.path.join(tempfile.gettempdir(), "ans_direct.txt")
        try:
            with contextlib.redirect_stdout(io.StringIO()), \
                    contextlib.redirect_stderr(io.StringIO()):
                main([orig, orig, ans])
            self.assertEqual(read_text(ans).strip(), "1.00")
        finally:
            os.remove(orig)
            if os.path.exists(ans):
                os.remove(ans)

    def test_unexpected_error_exits_4(self):
        with mock.patch("main.read_text", side_effect=RuntimeError("boom")):
            with self.assertRaises(SystemExit) as cm:
                with contextlib.redirect_stdout(io.StringIO()), \
                        contextlib.redirect_stderr(io.StringIO()):
                    main(["a.txt", "b.txt", "c.txt"])
        self.assertEqual(cm.exception.code, 4)


class TestMainEntry(unittest.TestCase):
    """真实命令行端到端测试（subprocess）。"""

    def test_end_to_end_output(self):
        """端到端：相同文件 → 答案文件输出 1.00。"""
        orig = _write_temp(BASE_TEXT)
        ans = os.path.join(tempfile.gettempdir(), "ans_e2e.txt")
        try:
            proc = subprocess.run(
                [sys.executable, MAIN_PY, orig, orig, ans],
                capture_output=True,
                text=True,
            )
            self.assertEqual(proc.returncode, 0)
            self.assertEqual(read_text(ans).strip(), "1.00")
        finally:
            os.remove(orig)
            if os.path.exists(ans):
                os.remove(ans)


if __name__ == "__main__":
    unittest.main()
