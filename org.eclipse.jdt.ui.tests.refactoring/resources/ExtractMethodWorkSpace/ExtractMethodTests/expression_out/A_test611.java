package expression_out;

public class A_test611 {
	class Inner {
	}
	public void foo() {
		Inner inner= extracted();
	}
	protected Inner extracted() {
		return /*[*/new Inner()/*]*/;
	}
}
