# 小学四则运算题目生成器（Java）

## 功能

- **生成题目**：`Main -n 10 -r 10` 生成 10 道数值 10 以内（不含 10）的题目，输出到当前目录 `Exercises.txt` / `Answers.txt`（UTF-8）。
- **判定答案**：`Main -e Exercises.txt -a Answers.txt` 判分，统计输出到 `Grade.txt`。
- 支持自然数、真分数（`3/5`）、带分数（`2'3/8`）、括号；每题 1~3 个运算符；无负数中间结果；除法结果必为真分数；一次运行内按「交换 + / × 左右操作数」语义查重。

## 目录结构

```
ArithmeticGenerator/
├─ src/
│  ├─ Main.java        # 命令行入口：参数解析与模式分发
│  ├─ Generator.java   # 随机生成 + 约束保证 + HashSet 查重
│  ├─ Expression.java  # 表达式树：求值 / canonical 查重串 / 无歧义输出
│  ├─ Fraction.java    # 有理数精确运算与格式解析
│  └─ Grader.java      # 判分：递归下降解析 + 统计 Grade.txt
├─ test/
│  └─ Check.java       # 自检：查重语义断言 + 10000 题回环/约束校验
└─ README.md
```

## 编译与运行（Windows PowerShell）

```powershell
# 编译（JDK 8+ 均可，开发用的是 JDK 23）
javac -encoding UTF-8 -d out src\*.java test\Check.java

# 生成 10 道数值 10 以内的题目（输出在当前目录）
java -cp out Main -n 10 -r 10

# 判分
java -cp out Main -e Exercises.txt -a Answers.txt

# 自检（查重语义断言 + 10000 题回环与约束校验）
java -cp out Check
```

> 若机器页面文件较小导致 JVM 启动失败，可加低内存参数：
> `java -Xshare:off -XX:+UseSerialGC -Xmx256m -cp out Main -n 10 -r 10`

## 参数说明

| 参数 | 含义 | 说明 |
| ---- | ---- | ---- |
| `-n` | 题目个数 | 正整数，必填（生成模式） |
| `-r` | 数值范围 | 正整数，必填；不给会报错并打印帮助。自然数、分母、真分数值均 < r |
| `-e` | 题目文件 | 判分模式，与 `-a` 同时给 |
| `-a` | 答案文件 | 判分模式，与 `-e` 同时给 |

输入输出文件均为 UTF-8 编码；判分器兼容 ASCII 运算符（`-`、`*`、`/`）与题目符号（`−`、`×`、`÷`），并自动忽略 UTF-8 BOM。
