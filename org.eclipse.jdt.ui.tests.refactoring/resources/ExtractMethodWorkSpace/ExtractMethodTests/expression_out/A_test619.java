package expression_out;

public class A_test619 {
	public void foo() {
		int f= new A[extracted()].length;
	}

	protected int extracted() {
		return /*[*/1 + 2/*]*/;
	}
}
