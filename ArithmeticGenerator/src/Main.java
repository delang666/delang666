import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 程序入口：小学四则运算题目生成器。
 *
 * 生成模式：  java Main -n 10 -r 10     -> 生成 Exercises.txt 与 Answers.txt
 * 判分模式：  java Main -e Exercises.txt -a Answers.txt -> 生成 Grade.txt
 */
public final class Main {

    public static void main(String[] args) {
        Map<String, String> options = parseArgs(args);
        if (options == null) {
            printUsageAndExit();
        }

        try {
            if (options.containsKey("-e") || options.containsKey("-a")) {
                runGrading(options);
            } else {
                runGeneration(options);
            }
        } catch (IllegalArgumentException | IOException e) {
            System.err.println("错误：" + e.getMessage());
            System.exit(1);
        }
    }

    /** 生成模式：-r 必须给定，否则报错并输出帮助。 */
    private static void runGeneration(Map<String, String> options) throws IOException {
        if (!options.containsKey("-r")) {
            System.err.println("错误：缺少必须参数 -r（数值范围）。");
            printUsageAndExit();
        }
        if (!options.containsKey("-n")) {
            System.err.println("错误：缺少必须参数 -n（题目个数）。");
            printUsageAndExit();
        }
        int n = parsePositiveInt(options.get("-n"), "-n");
        int r = parsePositiveInt(options.get("-r"), "-r");

        Generator generator = new Generator(r, new Random());
        List<Expression> problems = generator.generate(n);

        List<String> exerciseLines = new ArrayList<>(problems.size());
        List<String> answerLines = new ArrayList<>(problems.size());
        for (int i = 0; i < problems.size(); i++) {
            exerciseLines.add((i + 1) + ". " + problems.get(i).toDisplay() + " =");
            answerLines.add((i + 1) + ". " + problems.get(i).value());
        }
        Files.write(Paths.get("Exercises.txt"), exerciseLines, StandardCharsets.UTF_8);
        Files.write(Paths.get("Answers.txt"), answerLines, StandardCharsets.UTF_8);
        System.out.println("已生成 " + problems.size() + " 道题目：Exercises.txt / Answers.txt");
    }

    /** 判分模式：-e 与 -a 必须同时给定。 */
    private static void runGrading(Map<String, String> options) throws IOException {
        if (!options.containsKey("-e") || !options.containsKey("-a")) {
            System.err.println("错误：判分模式需要同时给定 -e <题目文件> 与 -a <答案文件>。");
            printUsageAndExit();
        }
        Grader.grade(options.get("-e"), options.get("-a"));
    }

    /** 解析 "-x value" 形式的命令行参数；格式非法返回 null。 */
    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String key = args[i];
            if (!key.startsWith("-") || i + 1 >= args.length || args[i + 1].startsWith("-")) {
                return null;
            }
            options.put(key, args[++i]);
        }
        return options;
    }

    private static int parsePositiveInt(String text, String name) {
        try {
            int v = Integer.parseInt(text);
            if (v < 1) {
                throw new IllegalArgumentException(name + " 必须是正整数，实际为 " + text);
            }
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " 必须是正整数，实际为 " + text);
        }
    }

    private static void printUsageAndExit() {
        System.err.println("用法：");
        System.err.println("  生成题目：java Main -n <题目个数> -r <数值范围>");
        System.err.println("    例如：java Main -n 10 -r 10  （生成 10 道数值在 10 以内的题目）");
        System.err.println("  判定答案：java Main -e <题目文件>.txt -a <答案文件>.txt");
        System.exit(1);
    }
}
