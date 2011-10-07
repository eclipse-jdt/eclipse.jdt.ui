package expression_out;

public class A_test623 {

	int i;

	public void foo() {
		this.i= /*[*/extracted();/*]*/
	}

	protected int extracted() {
		return 122;
	}
}
