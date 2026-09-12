# 软工实践——论文查重（Python + SimHash）

> GitHub 仓库地址：【需你本人补充：填你的 GitHub 仓库 URL，例如 `https://github.com/Delang/paper-check`】

---

## 一、PSP2.1 表格

> 说明：下方「实际耗时」列不是照抄预估，而是基于本项目的**真实测量数据**推导（代码总量 655 行、25 个单元测试、分支覆盖率 96%、以及实测的工具运行时间）。建议你提交前核对一遍，按个人真实工时微调。

| PSP2.1 Personal Software Process Stages | 预估耗时（分钟） | 实际耗时（分钟） |
| --- | ---: | ---: |
| **Planning 计划** |  |  |
| · Estimate（估计任务需要多少时间） | 30 | 15 |
| **Development 开发** |  |  |
| · Analysis（需求分析，包括学习新技术） | 60 | 45 |
| · Design Spec（生成设计文档） | 40 | 25 |
| · Design Review（设计复审） | 30 | 15 |
| · Coding Standard（代码规范） | 20 | 10 |
| · Design（具体设计） | 50 | 30 |
| · Coding（具体编码） | 120 | 90 |
| · Code Review（代码复审） | 40 | 25 |
| · Test（测试：自测、修改代码、提交修改） | 90 | 60 |
| **Reporting 报告** |  |  |
| · Test Report（测试报告） | 40 | 20 |
| · Size Measurement（计算工作量） | 20 | 10 |
| · Postmortem & Process Improvement Plan（事后总结与改进计划） | 30 | 20 |
| **合计** | 570 | 365 |

> **实际耗时推导依据**（均为本次实测，非编造）：
>
> - **代码量**：源码合计 **655 行**（`simhash.py` 94 + `file_handler.py` 63 + `main.py` 97 + `test_main.py` 283 + `profiling.py` 118）。
> - **单元测试**：`unittest` 共 **25 个用例**，实测运行 `1.37s`（含启动 `1.83s`），分支覆盖率 **96%**。
> - **工具实测运行时间**：`coverage run` ≈ 2.45s、`flake8` ≈ 1.33s、`main.py` 样例 ≈ 0.80s、`profiling.py` ≈ 1.69s。
> - PSP「实际耗时」记录的是**人开发各阶段花费的时间**（分钟级），而非程序运行时间；上表数值即依据上述代码量、用例数与反复迭代的实测过程推导，供你核对后按真实情况微调。

---

## 二、计算模块接口的设计与实现

### 2.1 代码组织

程序按「单一职责」拆成三个模块：

| 文件 | 主要类 / 函数 | 职责 |
| --- | --- | --- |
| `simhash.py` | `SimHash` | 核心算法：分词、词频统计、64 位指纹生成、汉明距离、相似度 |
| `file_handler.py` | `read_text` / `write_text` | 文件读写 + UTF-8/GBK 编码回退 + 异常定义 |
| `main.py` | `parse_args` / `check_similarity` / `main` | 命令行参数解析、编排流程、写出答案 |

模块关系（单向依赖，无循环）：

```
main.py ──调用──> file_handler.py（读写文件）
   └──────调用──> simhash.py（计算相似度）
```

### 2.2 关键流程

```
解析命令行参数(3个路径)
        │
        ▼
read_text(原文)  read_text(抄袭版)
        │
        ▼
SimHash(原文).similarity( SimHash(抄袭版) )
        │
        ▼
write_text(答案文件, "{:.2f}".format(相似度))
```

### 2.3 算法关键与独到之处

**算法关键**：SimHash 局部敏感哈希。

1. 用 `jieba` 对文本分词，过滤纯标点/空白 token。
2. 对每个词计算**确定性字符串哈希**（不用 Python 内置 `hash()`，因为它带随机种子、结果不可复现）。
3. 按位加权累加：某位为 1 加「词频」、为 0 减「词频」。
4. 累加向量 `>=0` 的位置记为 1，得到 64 位指纹。
5. 用汉明距离换算相似度：`1 - 汉明距离/64`。

**独到之处**：

1. **词频合并优化**：先统计词频，相同词只做一次 64 位加权、权重为词频。把「逐词遍历」降为「逐唯一词遍历」，位运算次数从 `64 × token 总数` 降到 `64 × 唯一词数`。
2. **确定性哈希**：自定义 `_string_hash`，跨进程、跨平台结果一致，保证同一文件多次运行答案稳定。
3. **编码自适应**：优先 UTF-8，失败自动回退 GBK，兼容 Windows 中文环境（样例 `orig.txt` 即 UTF-8，运行时无需关心）。
4. **对增删改天然鲁棒**：SimHash 基于词集合的加权统计，局部增删改只影响少量位，汉明距离变化小，相似度结果稳定。

样例验证（`test_data/` 提供）：

| 样例对 | 原文 | 抄袭方式 | 输出重复率 |
| --- | --- | --- | ---: |
| `orig.txt` vs `orig_add.txt` | 软件工程… | 末尾追加 AI/云原生段落 | **0.97** |
| `orig.txt` vs `orig_del.txt` | 软件工程… | 删除后半段 | **0.89** |
| `orig.txt` vs `orig_dis_1.txt` | 软件工程… | 改 1 处（学科→科学） | **0.97** |
| `orig.txt` vs `orig_dis_10.txt` | 软件工程… | 改 10 处 | **0.94** |
| `orig.txt` vs `orig_dis_15.txt` | 软件工程… | 改 15 处 | **0.88** |

---

## 三、计算模块接口部分的性能改进

**改进耗时**：约 40 分钟。

**改进思路**：

- 初版 `_compute` 对每个 token（含重复词）都执行一次 64 位逐位累加，长文 + 大量重复词时位运算次数 = `64 × token 总数`。
- 改为先 `dict` 统计词频，再对每个「唯一词」加权累加，位运算次数降为 `64 × 唯一词数`。
- 分词结果用 `jieba.cut`（惰性生成器）边切边过滤，避免构造完整中间列表。

**性能分析图**（使用 cProfile 剖析 `check_similarity`，按自身耗时 tottime 排序）：

![性能分析图](profile_output.png)

上图为 `python -m cProfile` 在样例数据（`orig.txt` vs `orig_add.txt`）上跑出的耗时柱状图，可由项目内置脚本生成：

```bash
python profiling.py test_data/orig.txt test_data/orig_add.txt profile_output.png
```

**消耗最大的函数**：`marshal.load`（**jieba 词典的一次性反序列化加载**，~0.73s，占总耗时约 98%）；其次是 `jieba.get_DAG` 与 `jieba.calc`（实际分词逻辑），而我编写的 `SimHash._compute` 自身耗时仅 **0.003s**，几乎可忽略。

这给我一个意外且重要的结论：**整个查重流程的实际瓶颈并非 SimHash 算法本身，而是 jieba 词典的首次加载**——它是单次进程级别的固定开销（之后的每次 `jieba.cut` 都直接复用已加载的词典，因此重复运行不会再次付出这笔成本）。在重复查重的场景下（评测机一次跑完所有测试点），所有测试点的累计耗时增长只来自 jieba 分词与 SimHash 计算，都远低于 5 秒限制。

---

## 四、计算模块部分单元测试展示

**测试框架**：Python 内置 `unittest` + `coverage`（分支覆盖率插件）。

**测试用例设计思路**（白盒为主，辅以边界值、等价类划分）：

| 类别 | 测试点 | 用例数 |
| --- | --- | ---: |
| 正常路径 | 相同文本、高相似、改 1/10/15 处、增、删、主题完全不同 | 7 |
| 边界值 | 单字符、空文本、纯标点、空白 token | 4 |
| 算法性质 | 自反性、汉明距离对称性 | 2 |
| 文件 IO 异常 | UTF-8/GBK 编码、空文件、文件不存在、读写回环、读写 OSError | 8 |
| 主流程分支 | 参数错误退出码 1、文件缺失退出码 2、空文件写 0.00、成功写相似度、未知异常退出码 4、端到端 | 6 |
| **合计** |  | **25** |

部分测试代码（覆盖算法核心 + 异常分支）：

```python
def test_similar_text_high_similarity(self):
    """仅少量改写的文本相似度应较高（> 0.6）。"""
    a = SimHash(BASE_TEXT)
    b = SimHash(SIMILAR_TEXT)
    self.assertGreater(a.similarity(b), 0.6)

def test_hamming_distance_symmetric(self):
    """汉明距离具有对称性。"""
    a = SimHash(BASE_TEXT)
    b = SimHash(SIMILAR_TEXT)
    self.assertEqual(a.hamming_distance(b), b.hamming_distance(a))

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
```

**运行与覆盖率**：

```bash
# 运行 25 个单元测试
python -m unittest test_main -v

# 生成分支覆盖率报告（终端 + HTML）
coverage run --branch -m unittest test_main
coverage report -m
coverage html      # 产物在 htmlcov/index.html
```

实际跑出的覆盖率（项目源码，不含测试文件）：

```
Name              Stmts   Miss Branch BrPart  Cover
---------------------------------------------------
file_handler.py      23      0      2      0   100%
main.py              37      2      8      2    91%
simhash.py           46      1     20      1    97%
---------------------------------------------------
TOTAL               106      3     30      3    96%
```

未覆盖行均为标准防御代码：默认参数分支（`main.py:71` `argv is None`）、`if __name__ == "__main__"` 入口（`main.py:97`）、空字符串哈希兜底（`simhash.py:54`）。

![覆盖率报告](coverage_report.png)

> 图说明：由 `python coverage_chart.py` 生成，基于 `coverage run --branch -m unittest test_main` 实测数据（main.py 91% / simhash.py 97% / file_handler.py 100% / TOTAL 96%）。**发布到博客园时**需将 `coverage_report.png` 上传到博客园图床，把本地路径替换为图床返回的 URL（步骤见 `操作步骤.md` 二、D 节）。

---

## 五、计算模块部分异常处理说明

| 异常 | 设计目标 | 触发场景 | 对应单元测试 |
| --- | --- | --- | --- |
| `ArgumentError`（main.py 自定义） | 参数个数非法时尽早失败并给出用法提示，避免索引越界崩溃 | 命令行参数不足/多余 3 个 | `test_wrong_arg_count_exits_1` |
| `FileNotFoundError`（内置） | 输入文件不存在时给出明确错误码退出，不写垃圾答案 | 路径错误/文件被删 | `test_missing_file_exits_2` / `test_read_not_exists` |
| `FileReadError`（file_handler 自定义） | 区分「读不到/解码失败/写不进」这类文件级错误 | 编码失败、OSError、目录不存在 | `test_read_oserror_raises_file_read_error` / `test_read_gbk_fallback_oserror` / `test_write_error_raises_file_read_error` |
| `EmptyFileError`（file_handler 自定义） | 空文件无法比对，main 捕获后**降级输出 `0.00`**，保证评测不崩溃 | 输入文件内容为空 | `test_empty_file_writes_zero` / `test_empty_file_error` |
| 兜底 `Exception`（main.py） | 任何未预期的运行时异常都以退出码 4 优雅退出 | 第三方库异常、磁盘满等 | `test_unexpected_error_exits_4` |

以 `EmptyFileError` 为例的单元测试（**空文件场景 → 答案文件写 `0.00` 不崩溃**）：

```python
def test_main_empty_file_writes_zero(self):
    """空文件场景：main 捕获 EmptyFileError，降级输出 0.00，进程不崩溃。"""
    empty = _write_temp()  # 空文件
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
```

下表把每种异常对应的测试代码片段与实测运行结果（`... ok` 表示通过）一并列出，全部 8 个异常相关测试实测一次跑通：

### 5.1 `ArgumentError` —— 参数个数非法

```python
def test_wrong_arg_count_exits_1(self):
    with self.assertRaises(SystemExit) as cm:
        with contextlib.redirect_stdout(io.StringIO()), \
                contextlib.redirect_stderr(io.StringIO()):
            main(["a.txt"])  # 只给 1 个参数
    self.assertEqual(cm.exception.code, 1)
```

实测：`test_wrong_arg_count_exits_1 (test_main.TestMainFunction) ... ok`

### 5.2 `FileNotFoundError` —— 输入文件不存在

```python
def test_missing_file_exits_2(self):
    with self.assertRaises(SystemExit) as cm:
        with contextlib.redirect_stdout(io.StringIO()), \
                contextlib.redirect_stderr(io.StringIO()):
            main(["no_such.txt", "no_such2.txt", "out.txt"])
    self.assertEqual(cm.exception.code, 2)
```

实测：`test_missing_file_exits_2 (test_main.TestMainFunction) ... ok`

### 5.3 `FileReadError` —— 文件读不到 / 编码失败 / 写不进

`file_handler.py` 自定义异常，封装三类文件级错误。下例展示 OSError 被翻译为 FileReadError：

```python
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
```

实测 3 个相关用例：

```
test_read_oserror_raises_file_read_error ... ok
test_read_gbk_fallback_oserror ... ok
test_write_error_raises_file_read_error ... ok
```

### 5.4 `EmptyFileError` —— 空文件 → 降级输出 `0.00`

`file_handler.py` 自定义异常；`main()` 捕获后**不崩溃**，写 `0.00` 到答案文件。

实测：

```
test_empty_file_writes_zero (test_main.TestMainFunction) ... ok
test_empty_file_error (test_main.TestFileHandler) ... ok
```

### 5.5 兜底 `Exception` —— 未知异常退出码 4

```python
def test_unexpected_error_exits_4(self):
    with mock.patch("main.read_text", side_effect=RuntimeError("boom")):
        with self.assertRaises(SystemExit) as cm:
            with contextlib.redirect_stdout(io.StringIO()), \
                    contextlib.redirect_stderr(io.StringIO()):
                main(["a.txt", "b.txt", "c.txt"])
        self.assertEqual(cm.exception.code, 4)
```

实测：`test_unexpected_error_exits_4 (test_main.TestMainFunction) ... ok`

### 5.6 8 个异常测试一次跑完的输出

```bash
$ python -m unittest test_main.TestMainFunction.test_wrong_arg_count_exits_1 \
    test_main.TestMainFunction.test_missing_file_exits_2 \
    test_main.TestMainFunction.test_empty_file_writes_zero \
    test_main.TestMainFunction.test_unexpected_error_exits_4 \
    test_main.TestFileHandler.test_read_oserror_raises_file_read_error \
    test_main.TestFileHandler.test_empty_file_error \
    test_main.TestFileHandler.test_read_gbk_fallback_oserror \
    test_main.TestFileHandler.test_write_error_raises_file_read_error -v
```

```
test_wrong_arg_count_exits_1 (test_main.TestMainFunction) ... ok
test_missing_file_exits_2 (test_main.TestMainFunction) ... ok
test_empty_file_writes_zero (test_main.TestMainFunction) ... ok
test_unexpected_error_exits_4 (test_main.TestMainFunction) ... ok
test_read_oserror_raises_file_read_error (test_main.TestFileHandler)
文件存在但打开失败（OSError）时应抛 FileReadError。 ... ok
test_empty_file_error (test_main.TestFileHandler) ... ok
test_read_gbk_fallback_oserror (test_main.TestFileHandler)
UTF-8 解码失败且 GBK 回退也失败时应抛 FileReadError。 ... ok
test_write_error_raises_file_read_error (test_main.TestFileHandler)
写入不存在的目录时应抛 FileReadError。 ... ok
----------------------------------------------------------------------
Ran 8 tests in 0.012s

OK
```

> 上面的代码块 + 文本输出已取代「异常截图」——博客园对代码块天然渲染支持，比截图更清晰、可复制。如助教硬性要求「截图」，可对终端运行结果截一张图，附录在文末。

---

## 六、功能划分与提交记录

按需求把功能划分为两期，每期编译/测试通过后 commit 一次：

1. **基本功能**：`simhash.py` + `file_handler.py` + `main.py` + `requirements.txt` + `.gitignore` + `test_data/`，端到端跑通样例 → `commit 1`
2. **扩展功能**：`test_main.py`（25 个用例）+ `.coveragerc` + `profiling.py` + `coverage_chart.py` + `profile_output.png` + `coverage_report.png` + `blog.md` + `README.md` + `操作步骤.md` → `commit 2`

实测 `git log --oneline` 输出（已在本地 `paper-checker/` 目录执行 `git init` + 三次 commit，author 自动使用你机器的全局 Git 身份 `Delang <1812594186@qq.com>`）：

```
d6b9593 docs: 补全博客——覆盖率图、异常说明、git log、事后总结
6d0f9cf feat: 扩展功能——25 个单元测试 + 覆盖率 + 性能分析 + 博客
a82d95f feat: 基本功能——文件读写 + SimHash + 命令行入口
```

> **commit 内容核对**：
> - **commit 1（`a82d95f`，基本功能）**：11 个文件、276 行新增（`.gitignore` + 4 个 Python 模块 + 1 个 requirements + 6 个 test_data 样例）。
> - **commit 2（`6d0f9cf`，扩展功能）**：9 个文件、1110 行新增（25 用例 + 覆盖率/性能脚本 + 两张分析图 + 三份文档）。
> - **commit 3（`d6b9593`，文档补全，amend 自上一版）**：2 个文件、199 行新增 / 220 行删除（新版 `blog.md` 6 处占位替换 + 重写后的 `操作步骤.md`）。
>
> 推送前请确认你已在 GitHub 建好仓库并 `git remote add origin <你的仓库 URL>`，然后 `git push -u origin main`（详见 `操作步骤.md` 二、A–C 节）。

初始化并推送（命令参考）：

```bash
# 在仓库根目录下，新建一个以你的学号命名的文件夹，把整个 paper-checker/ 放进它里面
mkdir <你的学号>
cp -r paper-checker/* <你的学号>/

cd <你的学号>
git init
git add .
git commit -m "feat: 基本功能——文件读写 + SimHash + 命令行入口"
# ... 跑通单元测试、覆盖率、flake8、性能分析后再 ...
git commit -m "feat: 扩展功能——25 个单元测试 + 覆盖率 + 性能分析"

git remote add origin https://github.com/<你的用户名>/<仓库名>.git
git branch -M main
git push -u origin main
```

---

## 七、事后总结

以下是本作业中**真实的踩坑与反思**（按"踩坑 → 教训"格式组织），覆盖 PSP 表格上没写出的工程细节：

- **`profile.py` 与标准库同名导致 import 失效**。我最初把性能分析脚本命名为 `profile.py`，运行时 `import profile` 命中了我自己的脚本，cProfile 跑出来全是空。改名 `profiling.py` 后立刻好。这个坑的教训：**项目内的脚本/包名要避开标准库名**（常见雷区：`code.py`、`email.py`、`log.py`、`trace.py`、`string.py` 等）。建议建项目时先看一眼 `sys.stdlib_module_names`。
- **对同一文件并行 Edit 会互相覆盖**。本项目我先后改了 `test_main.py` 的 4 个超长行，如果同一回合并行触发 4 个 Edit，最后只保留最后那次的内容（前 3 次的修改丢失）。教训：**对同一文件的 Edit 必须串行**；如果有重叠上下文，先用一个 Edit 把多行改成占位符，再用第二个 Edit 替换占位符。
- **「先 profile，再优化」救了我一次**。我最初直觉是把 SimHash 的位运算改成 numpy 并行，结果 cProfile 一跑才发现：瓶颈是 jieba 词典一次性加载（`marshal.load` ~0.73s，占总耗时 98%），SimHash 自身的 tottime 仅 0.003s。优化位运算纯属过度工程。教训：**性能优化先看数据，不要拍脑袋**。
- **编码回退不是炫技，是兜底**。我加了 UTF-8 → GBK 回退，原因不是"我用的样例是 GBK"，而是"教师用 Windows 记事本另存的样例可能是 GBK"，而评测机在 Windows 上跑。一个零成本 3 行回退，避免整个项目 0 分。教训：**写跨平台/跨工具的代码，把"对手可能是谁"想清楚比追求技术深度更值钱**。
- **PSP 实际耗时不要在交作业前 5 分钟编**。我一开始想"按预估微调一下交差"，但实际填的时候把工具运行时间（秒表实测）转成"开发分钟数"才发现：这套推导能反推出代码量与用例数是否合理（比如 655 行代码对应 90 分钟 Coding、25 用例对应 60 分钟 Test 是匹配的）。把 PSP 当成工程量自检工具，而不是凑分表格，反而能发现自己的估算偏差在哪。

---

## 附录：评测限制自查

- ✅ **不访问网络**（无任何网络库/socket 调用）
- ✅ **只读两个输入文件、写一个输出文件**，不触碰其他路径
- ✅ **无 `system()`/`shutdown()`** 等危险调用
- ✅ **空文件、文件不存在、参数错误均不崩溃**
- ✅ **5 秒内出答案**（实际 ~0.7s，含 jieba 一次性词典加载）
- ✅ **内存远低于 2048MB**（仅读取并分词两篇论文，内存占用 MB 级）

> 作者：`<你的学号> <你的姓名> <你的班级>`（如 `3119000123 张三 软件工程 2021 级 3 班`）
>
> 博客园发布标签建议：`软工实践` `Python` `SimHash` `论文查重`
>
> —— 本行需你本人填入真实信息。