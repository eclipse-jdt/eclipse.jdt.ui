package expression_out;

public class A_test618 {
	public void foo() {
		int i= 20 + extracted() + 10;
	}

	protected int extracted() {
		return /*[*/10 * 30/*]*/;
	}
}

