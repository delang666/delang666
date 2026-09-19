import java.util.List;
import java.util.Random;

/**
 * 自检程序（非交付必需，供开发自测与博文测试章节引用）。
 *
 * 覆盖三组不变量：
 *  1. 查重语义：与题目给出的例子逐一对应；
 *  2. 显示-解析回环：题目打印出来再被判分器解析，值必须不变；
 *  3. 生成约束：任意中间结果非负，除法结果必为真分数，运算符个数 1~3。
 */
public final class Check {

    public static void main(String[] args) {
        testCanonicalExamplesFromSpec();
        testGeneratedProblems();
        System.out.println("ALL CHECKS PASSED");
    }

    /** 题目原文中的查重例子：23+45 与 45+23 重复；(1+2)+3 与 3+(2+1) 重复、与 (3+2)+1 不重复。 */
    static void testCanonicalExamplesFromSpec() {
        Expression a = Expression.node('+', n(23), n(45));
        Expression b = Expression.node('+', n(45), n(23));
        assertEquals(a.canonical(), b.canonical(), "23+45 与 45+23 应判重");

        Expression p = Expression.node('+', Expression.node('+', n(1), n(2)), n(3));
        Expression q = Expression.node('+', n(3), Expression.node('+', n(2), n(1)));
        Expression r = Expression.node('+', Expression.node('+', n(3), n(2)), n(1));
        assertEquals(p.canonical(), q.canonical(), "(1+2)+3 与 3+(2+1) 应判重");
        assertNotEquals(p.canonical(), r.canonical(), "(1+2)+3 与 (3+2)+1 不应判重");

        Expression m1 = Expression.node('×', n(6), n(8));
        Expression m2 = Expression.node('×', n(8), n(6));
        assertEquals(m1.canonical(), m2.canonical(), "6×8 与 8×6 应判重");
    }

    /** 批量生成后校验回环与约束。 */
    static void testGeneratedProblems() {
        List<Expression> problems = new Generator(10, new Random(42)).generate(10000);
        assertEquals(10000, problems.size(), "应生成 10000 道题");
        for (Expression e : problems) {
            Fraction reparsed = Grader.evalExpression(e.toDisplay());
            assertEquals(e.value(), reparsed, "显示-解析回环不一致: " + e.toDisplay());
            checkConstraints(e, 0);
        }
        System.out.println("10000 道题回环与约束校验通过");
    }

    /** 递归校验：减法/任意中间结果非负、除法结果为真分数、运算符个数 <= 3。 */
    static void checkConstraints(Expression e, int depth) {
        if (e.isLeaf()) {
            return;
        }
        checkConstraints(e.getLeft(), depth + 1);
        checkConstraints(e.getRight(), depth + 1);
        if (e.value().compareTo(Fraction.of(0)) < 0) {
            throw new AssertionError("中间结果为负: " + e.toDisplay());
        }
        if (e.getOperator() == '÷' && !e.value().isProperFraction()) {
            throw new AssertionError("除法结果不是真分数: " + e.toDisplay());
        }
        if (countOps(e) > 3) {
            throw new AssertionError("运算符超过 3 个: " + e.toDisplay());
        }
    }

    static int countOps(Expression e) {
        return e.isLeaf() ? 0 : 1 + countOps(e.getLeft()) + countOps(e.getRight());
    }

    static Expression n(long v) {
        return Expression.leaf(Fraction.of(v));
    }

    static void assertEquals(Object expected, Object actual, String msg) {
        if (!expected.equals(actual)) {
            throw new AssertionError(msg + " | expected=" + expected + " actual=" + actual);
        }
    }

    static void assertNotEquals(Object a, Object b, String msg) {
        if (a.equals(b)) {
            throw new AssertionError(msg + " | both=" + a);
        }
    }
}
