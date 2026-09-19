import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 判分器：读取题目文件与答案文件，逐题对答案，统计结果写入 Grade.txt。
 *
 * 表达式用递归下降解析，求值全程使用 Fraction 精确计算；
 * 解析器同时兼容 ASCII（-, *, /）与题目符号（−, ×, ÷）。
 */
public final class Grader {

    /** 对 exercisefile 与 answerfile 判分，结果写入当前目录 Grade.txt。 */
    public static void grade(String exerciseFile, String answerFile) throws IOException {
        List<String> exercises = Files.readAllLines(Paths.get(exerciseFile), StandardCharsets.UTF_8);
        List<String> answers = Files.readAllLines(Paths.get(answerFile), StandardCharsets.UTF_8);

        List<Integer> correct = new ArrayList<>();
        List<Integer> wrong = new ArrayList<>();
        int total = Math.min(exercises.size(), answers.size());
        for (int i = 0; i < total; i++) {
            int no = i + 1;   // 题目按顺序编号
            try {
                Fraction expected = evalExpression(stripIndex(exercises.get(i)));
                Fraction actual = Fraction.parse(stripIndex(answers.get(i)));
                (expected.equals(actual) ? correct : wrong).add(no);
            } catch (RuntimeException ex) {
                wrong.add(no);  // 解析失败的按错误处理，不中断整体判分
            }
        }

        List<String> out = new ArrayList<>();
        out.add(formatLine("Correct", correct));
        out.add(formatLine("Wrong", wrong));
        Files.write(Paths.get("Grade.txt"), out, StandardCharsets.UTF_8);
        System.out.println("判分完成：" + out.get(0));
        System.out.println("判分完成：" + out.get(1));
        System.out.println("结果已写入 Grade.txt");
    }

    /** 输出行格式：Correct: 5 (1, 3, 5, 7, 9) */
    private static String formatLine(String label, List<Integer> numbers) {
        StringBuilder sb = new StringBuilder(label).append(": ").append(numbers.size()).append(" (");
        for (int i = 0; i < numbers.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(numbers.get(i));
        }
        return sb.append(")").toString();
    }

    /** 去掉行首 BOM、题号 "12." 与行尾等号。 */
    private static String stripIndex(String line) {
        String s = line.replace("\uFEFF", "").trim();
        int dot = s.indexOf('.');
        if (dot > 0 && s.substring(0, dot).chars().allMatch(Character::isDigit)) {
            s = s.substring(dot + 1).trim();
        }
        if (s.endsWith("=")) {
            s = s.substring(0, s.length() - 1).trim();
        }
        return s;
    }

    /** 解析并求值一个表达式字符串（空格分隔，支持括号与分数）。 */
    static Fraction evalExpression(String expr) {
        Parser p = new Parser(expr.split("\\s+"));
        Fraction v = p.parseExpression();
        if (!p.atEnd()) {
            throw new IllegalArgumentException("表达式存在多余内容: " + expr);
        }
        return v;
    }

    /** 递归下降解析器：expression -> term (('+'|'-') term)* 等。 */
    private static final class Parser {
        private final String[] tokens;
        private int pos;

        Parser(String[] tokens) {
            this.tokens = tokens;
        }

        boolean atEnd() {
            return pos >= tokens.length;
        }

        private String peek() {
            return atEnd() ? null : tokens[pos];
        }

        Fraction parseExpression() {
            Fraction v = parseTerm();
            while ("+".equals(peek()) || "-".equals(peek()) || "−".equals(peek())) {
                String op = tokens[pos++];
                Fraction r = parseTerm();
                v = "+".equals(op) ? v.add(r) : v.subtract(r);
            }
            return v;
        }

        private Fraction parseTerm() {
            Fraction v = parseFactor();
            while ("×".equals(peek()) || "*".equals(peek())
                    || "÷".equals(peek()) || "/".equals(peek())) {
                String op = tokens[pos++];
                Fraction r = parseFactor();
                v = ("×".equals(op) || "*".equals(op)) ? v.multiply(r) : v.divide(r);
            }
            return v;
        }

        private Fraction parseFactor() {
            String t = tokens[pos++];
            if ("(".equals(t)) {
                Fraction v = parseExpression();
                if (atEnd() || !")".equals(tokens[pos++])) {
                    throw new IllegalArgumentException("括号不匹配");
                }
                return v;
            }
            return Fraction.parse(t);
        }
    }
}
