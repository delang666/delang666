import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 题目生成器。
 *
 * 生成策略：随机生成含 1~3 个运算符的表达式树，在构造每个内部节点时
 * 就保证约束成立（减法不够减则交换左右、除法倒除则交换、无法挽救则
 * 退化为加法），再用 canonical() 规范串放入 HashSet 查重。
 */
public final class Generator {

    private final int range;   // 数值范围：自然数、真分数（含分母）均 < range
    private final Random random;

    public Generator(int range, Random random) {
        this.range = range;
        this.random = random;
    }

    /**
     * 生成 count 道互不重复的题目。
     *
     * 若组合空间耗尽（如 -r 很小而 -n 很大），返回已生成的部分并告警。
     */
    public List<Expression> generate(int count) {
        List<Expression> result = new ArrayList<>(count);
        Set<String> seen = new HashSet<>(count * 2);
        long maxAttempts = count * 200L + 1000;   // 防止 -r 过小时死循环
        long attempts = 0;
        while (result.size() < count && attempts < maxAttempts) {
            attempts++;
            Expression e = randomExpression(1 + random.nextInt(3)); // 1~3 个运算符
            if (seen.add(e.canonical())) {
                result.add(e);
            }
        }
        if (result.size() < count) {
            System.err.println("警告：当前 -r " + range + " 下不重复题目组合不足，"
                    + "仅生成 " + result.size() + " 道（请求 " + count + " 道）。请增大 -r。");
        }
        return result;
    }

    /** 随机生成一棵含 opCount 个运算符、且满足全部约束的表达式树。 */
    private Expression randomExpression(int opCount) {
        if (opCount == 0) {
            return Expression.leaf(randomOperand());
        }
        int leftOps = random.nextInt(opCount);            // 左右子树随机分配运算符个数
        Expression left = randomExpression(leftOps);
        Expression right = randomExpression(opCount - 1 - leftOps);
        return combine(left, right);
    }

    /**
     * 随机选运算符合并两棵子树，并在合并时保证约束：
     *  - 减法：若左 < 右则交换，保证中间结果不为负；
     *  - 除法：要求结果为正的真分数，左右颠倒则交换，实在不满足退化为加法。
     */
    private Expression combine(Expression left, Expression right) {
        char op = "+-×÷".charAt(random.nextInt(4));
        Fraction l = left.value();
        Fraction r = right.value();
        switch (op) {
            case '-':
                return l.compareTo(r) >= 0
                        ? Expression.node('-', left, right)
                        : Expression.node('-', right, left);
            case '÷':
                if (l.isZero() || r.isZero() || l.compareTo(r) == 0) {
                    return Expression.node('+', left, right); // 无法构成真分数，退化为加法
                }
                return l.compareTo(r) < 0
                        ? Expression.node('÷', left, right)
                        : Expression.node('÷', right, left);
            default:
                return Expression.node(op, left, right);
        }
    }

    /**
     * 随机操作数：自然数 [0, range)、真分数 num/den（den < range），
     * 以及少量带分数（题目说明中 1'1/2 也算真分数），值均 < range。
     */
    private Fraction randomOperand() {
        if (range < 3) {                       // 范围太小，只能给自然数
            return Fraction.of(random.nextInt(range));
        }
        int kind = random.nextInt(10);
        if (kind < 5) {                        // 50% 自然数
            return Fraction.of(random.nextInt(range));
        }
        int denominator = 2 + random.nextInt(range - 2);   // [2, range-1]
        int numerator = 1 + random.nextInt(denominator - 1); // [1, den-1]
        if (kind < 8) {                        // 30% 真分数
            return Fraction.of(numerator, denominator);
        }
        int whole = 1 + random.nextInt(range - 1);  // 20% 带分数 a'b/c
        return Fraction.of((long) whole * denominator + numerator, denominator);
    }
}
