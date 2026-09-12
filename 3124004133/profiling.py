# -*- coding: utf-8 -*-
"""性能分析脚本。

使用 cProfile 剖析论文查重流程，打印耗时排名并生成性能分析图。

运行：
    python profile.py <原文路径> <抄袭版路径> [输出图片名]

示例：
    python profile.py orig.txt orig_add.txt profile_output.png

产物：
    1. 控制台打印 pstats 耗时排名（可截图作为性能分析图）。
    2. 生成一张「耗时最多的函数」横向柱状图 PNG（按自身耗时 tottime 统计）。
"""

import cProfile
import pstats
import sys

import matplotlib

matplotlib.use("Agg")  # noqa: E402 必须在 pyplot import 前设置后端
import matplotlib.pyplot as plt  # noqa: E402

# 过滤掉框架/环境噪音，只保留项目源码、jieba 与有意义的耗时项
_NOISE = ("importlib", "<frozen", "sitecustomize", "builtins.exec")

# 内置函数名重映射，让图表更易读
_RELABEL = {
    "built-in method marshal.load": "marshal.load（jieba 词典一次性加载）",
    "built-in method _io.open": "_io.open（文件 IO）",
}


def profile_flow(orig_path, copy_path, repeats=1):
    """对查重流程做性能剖析，返回 pstats.Stats。"""
    from main import check_similarity

    profiler = cProfile.Profile()
    profiler.enable()
    for _ in range(repeats):
        check_similarity(orig_path, copy_path)
    profiler.disable()
    return pstats.Stats(profiler)


def _short_name(key):
    """把 (文件名, 行号, 函数名) 转成易读短名。"""
    filename, _lineno, funcname = key
    # 内置方法（C 扩展）：filename 为 "~" 或 "{built-in method ...}"
    if filename == "~" or filename.startswith("{"):
        if funcname.startswith("<") and funcname.endswith(">"):
            return funcname[1:-1]
        return funcname
    # 普通 .py 文件
    parts = filename.replace("\\", "/").split("/")
    basename = parts[-1]
    if basename == "__init__.py":
        # __init__.py 用父目录名（包名）作为模块名，避免双下划线被 matplotlib 当下标
        module = parts[-2]
    else:
        module = basename[:-3] if basename.endswith(".py") else basename
    return "{}.{}".format(module, funcname)


def build_chart(stats, out_path, top_n=10):
    """根据 stats 的自身耗时（tottime）生成柱状图。"""
    entries = []
    for key, value in stats.stats.items():
        filename = key[0]
        if any(noise in filename for noise in _NOISE):
            continue
        _cc, _nc, tt, _ct, _callers = value
        name = _short_name(key)
        name = _RELABEL.get(name, name)
        entries.append((name, tt))
    entries.sort(key=lambda x: -x[1])
    # 去掉几乎为 0 的条目，让图表更聚焦
    entries = [(n, t) for n, t in entries if t >= 0.0005]
    entries = entries[:top_n]

    # 倒序，让耗时最大的排在最上面
    names = [e[0] for e in entries][::-1]
    times = [e[1] for e in entries][::-1]

    plt.rcParams["font.sans-serif"] = ["Microsoft YaHei", "SimHei"]
    plt.rcParams["axes.unicode_minus"] = False
    fig, ax = plt.subplots(figsize=(10, 6))
    bars = ax.barh(names, times, color="#4C72B0")
    ax.set_xlabel("自身耗时 tottime（秒）")
    ax.set_title("论文查重性能分析 — 耗时最多的函数")
    ax.bar_label(bars, fmt="%.3f", padding=2)
    plt.tight_layout()
    plt.savefig(out_path, dpi=150)
    plt.close(fig)
    print("已生成性能分析图: {}".format(out_path))


def main():
    if len(sys.argv) < 3:
        print("用法: python profile.py <原文路径> <抄袭版路径> [输出图片名]")
        sys.exit(1)
    orig_path, copy_path = sys.argv[1], sys.argv[2]
    out_path = sys.argv[3] if len(sys.argv) > 3 else "profile_output.png"

    stats = profile_flow(orig_path, copy_path)
    stats.sort_stats("tottime")
    print("=" * 70)
    print("耗时最多的函数（按自身耗时 tottime 排序）")
    print("=" * 70)
    stats.print_stats(15)

    build_chart(stats, out_path)


if __name__ == "__main__":
    main()
