//selection: 8, 16, 8, 24
//name: i -> second
package simple.out;

public class ConstantExpression1 {
	public static final int ZERO= -1;
	public int m(int a, int second) {
		int b= second;
		return m(3 * a, second);
	}
	public void use() {
		m(17, ZERO - 2);
		m(17 * m(18, ZERO - 2), ZERO - 2);
	}
}
