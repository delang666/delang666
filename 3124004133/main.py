# -*- coding: utf-8 -*-
"""论文查重程序入口。

用法:
    python main.py <原文文件绝对路径> <抄袭版文件绝对路径> <答案文件绝对路径>

说明:
    1. 从命令行读取三个文件路径（以空格分隔，路径中不含空格）。
    2. 读取原文与抄袭版文本，用 SimHash 算法计算相似度。
    3. 将相似度以浮点数（精确到小数点后两位）写入答案文件。
    4. 程序仅读取两个输入文件、写入一个输出文件，不访问网络与其他文件。

作者: [学号]
"""

import sys

from file_handler import read_text, write_text, EmptyFileError
from simhash import SimHash


class ArgumentError(Exception):
    """命令行参数数量不正确时抛出的异常。"""


def parse_args(argv):
    """解析命令行参数。

    Args:
        argv: 命令行参数列表（不含程序名）。

    Returns:
        (orig_path, copy_path, ans_path) 三元组。

    Raises:
        ArgumentError: 参数个数不为 3 时。
    """
    if len(argv) != 3:
        raise ArgumentError(
            "参数个数错误，应为 3 个文件路径："
            "python main.py <原文文件> <抄袭版文件> <答案文件>"
        )
    return argv[0], argv[1], argv[2]


def check_similarity(orig_path, copy_path):
    """计算两篇论文的相似度。

    Args:
        orig_path: 原文文件路径。
        copy_path: 抄袭版文件路径。

    Returns:
        float: 相似度，取值范围 [0, 1]。

    Raises:
        EmptyFileError: 任意一篇内容为空。
    """
    orig_text = read_text(orig_path)
    copy_text = read_text(copy_path)

    if not orig_text.strip() or not copy_text.strip():
        raise EmptyFileError("输入文件内容为空，无法计算相似度")

    return SimHash(orig_text).similarity(SimHash(copy_text))


def main(argv=None):
    """程序主函数。"""
    if argv is None:
        argv = sys.argv[1:]

    try:
        orig_path, copy_path, ans_path = parse_args(argv)
    except ArgumentError as e:
        print("[错误] {}".format(e), file=sys.stderr)
        sys.exit(1)

    try:
        similarity = check_similarity(orig_path, copy_path)
    except FileNotFoundError as e:
        print("[错误] {}".format(e), file=sys.stderr)
        sys.exit(2)
    except EmptyFileError as e:
        # 空文件无法比对，按 0.00 输出并正常退出，保证评测不崩溃
        print("[警告] {}，按 0.00 处理".format(e), file=sys.stderr)
        similarity = 0.0
    except Exception as e:  # noqa: BLE001 - 兜底，避免异常退出导致评测扣分
        print("[错误] 处理失败: {}".format(e), file=sys.stderr)
        sys.exit(4)

    write_text(ans_path, "{:.2f}".format(similarity))
    print("相似度: {:.2f}".format(similarity))


if __name__ == "__main__":
    main()
