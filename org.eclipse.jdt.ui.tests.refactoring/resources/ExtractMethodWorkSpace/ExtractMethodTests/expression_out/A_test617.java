package expression_out;

public class A_test617 {
	public int foo() {
		return 10 + extracted() + 10;
	}

	protected int extracted() {
		return /*[*/20 * 30/*]*/;
	}
}

