//selection: 8, 16, 8, 24
//name: i -> second
package simple;

public class ConstantExpression1 {
	public static final int ZERO= -1;
	public int m(int a) {
		int b= ZERO - 2;
		return m(3 * a);
	}
	public void use() {
		m(17);
		m(17 * m(18));
	}
}
