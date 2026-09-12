# 论文查重（软件工程作业）

> 提交前请把本目录（`paper-checker`）**重命名为你的学号**，作为 GitHub 仓库中的学号文件夹。
> 例：`paper-checker/` → `3119000123/`

基于 **Python 3 + SimHash** 的论文查重程序。给定原文与抄袭版两个文本文件，输出二者的重复率（浮点数，精确到小数点后两位）。

## 目录结构

```
paper-checker/
├── main.py            # 程序入口：解析命令行参数、计算相似度、写出答案
├── simhash.py         # 核心算法：SimHash 指纹 + 汉明距离 + 相似度
├── file_handler.py    # 文件读写 + 编码回退 + 异常定义
├── test_main.py       # 单元测试（25 个用例，覆盖正常/异常/边界/算法性质）
├── profiling.py       # 性能分析脚本（cProfile + matplotlib 输出耗时图）
├── profile_output.png # 性能分析图（运行 profiling.py 自动生成）
├── .coveragerc        # 覆盖率配置：只统计项目源码
├── requirements.txt   # 依赖：jieba
├── htmlcov/           # 覆盖率 HTML 报告（运行 coverage html 生成）
└── blog.md            # 作业博客正文
└── test_data/         # 测试样例（orig + add/del/dis_1/dis_10/dis_15）
```

## 快速开始

```bash
# 1. 安装依赖（建议先激活虚拟环境）
pip install -r requirements.txt

# 2. 运行（路径中不含空格）
python main.py <原文文件绝对路径> <抄袭版文件绝对路径> <答案文件绝对路径>

# 示例：跑项目自带样例
python main.py test_data/orig.txt test_data/orig_add.txt test_data/ans.txt
#  → 答案文件 test_data/ans.txt 中写入 0.97
```

## 单元测试与分支覆盖率

```bash
# 运行 25 个单元测试
python -m unittest test_main -v

# 安装 coverage（如未安装）：pip install coverage

# 分支覆盖率（terminal + HTML）
coverage run --branch -m unittest test_main
coverage report -m
coverage html
```

实测覆盖率（仅项目源码）：

| 模块 | Stmts | Miss | Branch | BrPart | Cover |
| --- | ---: | ---: | ---: | ---: | ---: |
| `file_handler.py` | 23 | 0 | 2 | 0 | **100%** |
| `main.py` | 37 | 2 | 8 | 2 | **91%** |
| `simhash.py` | 46 | 1 | 20 | 1 | **97%** |
| **TOTAL** | 106 | 3 | 30 | 3 | **96%** |

## 性能分析

```bash
# 安装 matplotlib（如未安装）：pip install matplotlib

# 用 cProfile 剖析查重流程，生成耗时最多的函数柱状图
python profiling.py test_data/orig.txt test_data/orig_add.txt profile_output.png
```

实际剖析结果：`marshal.load`（jieba 词典一次性加载）~0.73s 占绝对主导，SimHash 计算本身 tottime 仅 0.003s。

## 代码质量分析

```bash
# 安装 flake8（如未安装）：pip install flake8

flake8 simhash.py file_handler.py main.py test_main.py
# 当前为 0 警告
```

## GitHub 签入建议（按功能划分，至少两次 commit）

```bash
git init
git add .
git commit -m "feat: 基本功能——文件读写 + SimHash + 命令行入口"   # commit 1

# ... 完成单元测试、覆盖率、性能分析后 ...
git commit -m "feat: 扩展功能——25 个单元测试 + 覆盖率 + 性能分析"  # commit 2

git remote add origin https://github.com/<你的用户名>/<仓库名>.git
git push -u origin main
```

## 评测限制自查

- ✅ 不访问网络（无任何网络库/socket 调用）
- ✅ 只读两个输入文件、写一个输出文件，不触碰其他路径
- ✅ 无 `system()`/`shutdown()` 等危险调用
- ✅ 空文件、文件不存在、参数错误、未知异常均不崩溃（按场景退出码 1/2/4）
- ✅ 5 秒内出答案（实测 ~0.7s）
- ✅ 内存远低于 2048MB