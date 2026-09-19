/**
 * 算术表达式二叉树。
 *
 * 叶子节点：自然数或真分数；内部节点：+, -, ×, ÷。
 * 核心职责：
 *   1. 求值（value）——交给 Fraction 精确计算；
 *   2. 规范化（canonical）——把「有限次交换 + 和 × 左右操作数」视为同一题的
 *      等价类映射到唯一字符串，用于查重；
 *   3. 输出（toDisplay）——按优先级补必要的括号，保证人类/判分器重新
 *      解析后与原树同值，运算符与等号前后均有空格。
 */
public final class Expression {

    private final Fraction leafValue;  // 叶子节点的数值；内部节点为 null
    private final char operator;       // 内部节点运算符；叶子为 '\0'
    private final Expression left;
    private final Expression right;

    private Expression(Fraction leafValue, char operator, Expression left, Expression right) {
        this.leafValue = leafValue;
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    public static Expression leaf(Fraction value) {
        return new Expression(value, '\0', null, null);
    }

    public static Expression node(char operator, Expression left, Expression right) {
        return new Expression(null, operator, left, right);
    }

    public boolean isLeaf() {
        return leafValue != null;
    }

    public char getOperator() {
        return operator;
    }

    public Expression getLeft() {
        return left;
    }

    public Expression getRight() {
        return right;
    }

    /** 表达式的精确值。 */
    public Fraction value() {
        if (isLeaf()) {
            return leafValue;
        }
        Fraction l = left.value();
        Fraction r = right.value();
        switch (operator) {
            case '+': return l.add(r);
            case '-': return l.subtract(r);
            case '×': return l.multiply(r);
            case '÷': return l.divide(r);
            default: throw new IllegalStateException("未知运算符: " + operator);
        }
    }

    /** 运算符优先级：叶子最高，× ÷ 次之，+ - 最低。 */
    private int precedence() {
        if (isLeaf()) {
            return 3;
        }
        return (operator == '×' || operator == '÷') ? 2 : 1;
    }

    /**
     * 规范化串：同一等价类（仅交换 + / × 左右操作数）映射到同一字符串。
     *
     * 做法：自底向上，遇到 + 或 × 就按字典序排列左右子树的规范串，
     * 并用括号保留树形结构。例如 (1+2)+3 与 3+(2+1) 的规范串相同，
     * 而 (1+2)+3 与 (3+2)+1 不同——与题目给出的查重定义一致。
     */
    public String canonical() {
        if (isLeaf()) {
            return leafValue.toString();
        }
        String l = left.canonical();
        String r = right.canonical();
        if ((operator == '+' || operator == '×') && l.compareTo(r) > 0) {
            String t = l;
            l = r;
            r = t;
        }
        return "(" + l + operator + r + ")";
    }

    /** 供写入 Exercises.txt 的展示形式（不含等号）。 */
    public String toDisplay() {
        if (isLeaf()) {
            return leafValue.toString();
        }
        return render(left, true) + " " + operator + " " + render(right, false);
    }

    /**
     * 子树展示：必要时加括号，保证字符串被按左结合重新解析后与原树同值。
     * 规则：子树优先级低于父节点必加；同级且位于右侧一律加括号
     * （避免 a+(b-c) 被误读为 (a+b)-c）。
     */
    private String render(Expression child, boolean isLeftChild) {
        String s = child.toDisplay();
        if (child.isLeaf()) {
            return s;
        }
        boolean needParen;
        if (child.precedence() != precedence()) {
            needParen = child.precedence() < precedence();
        } else {
            needParen = !isLeftChild;
        }
        return needParen ? "( " + s + " )" : s;
    }
}
