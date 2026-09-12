# -*- coding: utf-8 -*-
"""生成分支覆盖率可视化图。

把 coverage run --branch 实测得到的覆盖率数据画成柱状图，
作为博客「单元测试覆盖率」小节的可视化证据。

运行：
    python coverage_chart.py

产物：
    coverage_report.png
"""

import matplotlib

matplotlib.use("Agg")  # noqa: E402 必须在 pyplot import 前设置后端
import matplotlib.pyplot as plt  # noqa: E402

# 真实测得数据（coverage run --branch -m unittest test_main）
# 顺序对应柱状图自下而上
MODULES = ["main.py", "simhash.py", "file_handler.py", "TOTAL（合计）"]
COVERAGE = [91.0, 97.0, 100.0, 96.0]
STMT_COUNT = ["37 语句", "46 语句", "23 语句", "106 语句"]
COLORS = ["#DAA520", "#2E8B57", "#2E8B57", "#4C72B0"]


def main():
    plt.rcParams["font.sans-serif"] = ["Microsoft YaHei", "SimHei"]
    plt.rcParams["axes.unicode_minus"] = False

    fig, ax = plt.subplots(figsize=(8, 5))
    names = MODULES[::-1]
    vals = COVERAGE[::-1]
    stmts = STMT_COUNT[::-1]
    colors = COLORS[::-1]

    bars = ax.barh(names, vals, color=colors)
    ax.set_xlim(0, 115)
    ax.set_xlabel("分支覆盖率（%）")
    ax.set_title("论文查重单元测试分支覆盖率\n(coverage run --branch -m unittest test_main)")

    for bar, value, stmt in zip(bars, vals, stmts):
        ax.text(
            bar.get_width() + 1.5,
            bar.get_y() + bar.get_height() / 2,
            "{:.0f}%  {}".format(value, stmt),
            va="center",
            fontweight="bold",
        )

    # 90% 参考线
    ax.axvline(90, color="#888888", linestyle="--", linewidth=0.8)
    ax.text(90.5, -0.28, "90% 目标线", color="#888888", fontsize=8)

    plt.tight_layout()
    plt.savefig("coverage_report.png", dpi=150)
    plt.close(fig)
    print("已生成 coverage_report.png")


if __name__ == "__main__":
    main()
