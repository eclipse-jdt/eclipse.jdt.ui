package expression_out;

public class A_test609 {
	public void foo() {
		boolean b;
		b= 1 < 10 && extracted();
	}

	protected boolean extracted() {
		return /*[*/2 < 20/*]*/;
	}
}
