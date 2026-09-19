/**
 * 不可变有理数，用于自然数与真分数的精确运算。
 *
 * 说明：全程使用 Fraction 精确表示，避免 double 精度误差导致判分错误。
 * 规格：denominator 恒为正，分子可正可负（生成逻辑保证最终结果非负）。
 */
public final class Fraction implements Comparable<Fraction> {

    private final long numerator;    // 分子
    private final long denominator;  // 分母，恒 > 0

    private Fraction(long numerator, long denominator) {
        if (denominator == 0) {
            throw new ArithmeticException("分母不能为 0");
        }
        if (denominator < 0) {          // 符号统一放到分子上
            numerator = -numerator;
            denominator = -denominator;
        }
        long g = gcd(Math.abs(numerator), denominator);
        this.numerator = numerator / g;
        this.denominator = denominator / g;
    }

    /** 构造自然数。 */
    public static Fraction of(long value) {
        return new Fraction(value, 1);
    }

    /** 构造分数 numerator/denominator，自动约分。 */
    public static Fraction of(long numerator, long denominator) {
        return new Fraction(numerator, denominator);
    }

    /** 解析题目/答案文件中的数字：自然数 "5"、真分数 "3/5"、带分数 "2'3/8"。 */
    public static Fraction parse(String token) {
        token = token.trim();
        if (token.contains("'")) {                    // 带分数 a'b/c
            String[] whole = token.split("'");
            String[] frac = whole[1].split("/");
            long a = Long.parseLong(whole[0]);
            long b = Long.parseLong(frac[0]);
            long c = Long.parseLong(frac[1]);
            return new Fraction(a * c + b, c);
        }
        if (token.contains("/")) {                    // 真分数 b/c
            String[] frac = token.split("/");
            return new Fraction(Long.parseLong(frac[0]), Long.parseLong(frac[1]));
        }
        return new Fraction(Long.parseLong(token), 1); // 自然数
    }

    public Fraction add(Fraction o) {
        return new Fraction(numerator * o.denominator + o.numerator * denominator,
                denominator * o.denominator);
    }

    public Fraction subtract(Fraction o) {
        return new Fraction(numerator * o.denominator - o.numerator * denominator,
                denominator * o.denominator);
    }

    public Fraction multiply(Fraction o) {
        return new Fraction(numerator * o.numerator, denominator * o.denominator);
    }

    public Fraction divide(Fraction o) {
        return new Fraction(numerator * o.denominator, denominator * o.numerator);
    }

    /** 是否为零。 */
    public boolean isZero() {
        return numerator == 0;
    }

    /** 是否为真分数（0 < 值 < 1），用于约束 e1 ÷ e2 的结果。 */
    public boolean isProperFraction() {
        return numerator > 0 && numerator < denominator;
    }

    @Override
    public int compareTo(Fraction o) {
        return Long.compare(numerator * o.denominator, o.numerator * denominator);
    }

    /**
     * 按作业要求的格式输出：
     * 整数 -> "5"；真分数 -> "3/5"；带分数 -> "2'3/8"；零 -> "0"。
     */
    @Override
    public String toString() {
        if (numerator == 0) {
            return "0";
        }
        if (denominator == 1) {
            return Long.toString(numerator);
        }
        long abs = Math.abs(numerator);
        if (abs < denominator) {
            return numerator + "/" + denominator;
        }
        long whole = numerator / denominator;
        long rest = abs % denominator;
        return whole + "'" + rest + "/" + denominator;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Fraction)) {
            return false;
        }
        Fraction f = (Fraction) o;
        return numerator == f.numerator && denominator == f.denominator;
    }

    @Override
    public int hashCode() {
        return (int) (31 * numerator + denominator);
    }

    private static long gcd(long a, long b) {
        while (b != 0) {
            long t = a % b;
            a = b;
            b = t;
        }
        return a == 0 ? 1 : a;
    }
}
