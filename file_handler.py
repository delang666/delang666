# -*- coding: utf-8 -*-
"""文件读写模块。

负责论文原文与抄袭版文件的读取，以及答案文件的写入。
集中处理文件不存在、编码错误、空文件等异常情况。
"""

import os


class FileReadError(Exception):
    """文件读取或解码失败时抛出的异常。"""


class EmptyFileError(Exception):
    """文件内容为空时抛出的异常。"""


def read_text(path):
    """读取文本文件内容，优先按 UTF-8 解码，失败时回退 GBK。

    Args:
        path: 文件绝对路径。

    Returns:
        str: 文件内容。

    Raises:
        FileNotFoundError: 文件不存在。
        FileReadError: 读取或解码失败。
    """
    if not os.path.exists(path):
        raise FileNotFoundError("文件不存在: {}".format(path))

    try:
        with open(path, "r", encoding="utf-8") as f:
            return f.read()
    except UnicodeDecodeError:
        # 中文环境常见 GBK/GB18030 编码，回退解码
        try:
            with open(path, "r", encoding="gbk", errors="ignore") as f:
                return f.read()
        except OSError as e:
            raise FileReadError("读取文件失败: {}".format(e))
    except OSError as e:
        raise FileReadError("读取文件失败: {}".format(e))


def write_text(path, content):
    """将结果写入答案文件（UTF-8 编码）。

    Args:
        path: 答案文件绝对路径。
        content: 要写入的内容。

    Raises:
        FileReadError: 写入失败。
    """
    try:
        with open(path, "w", encoding="utf-8") as f:
            f.write(content)
    except OSError as e:
        raise FileReadError("写入答案文件失败: {}".format(e))
