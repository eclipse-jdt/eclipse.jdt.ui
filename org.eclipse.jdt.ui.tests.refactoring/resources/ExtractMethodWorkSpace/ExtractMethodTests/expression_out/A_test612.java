package expression_out;

public class A_test612 {
	class Inner {
	}
	public void foo() {
		Inner[] inner= extracted();
	}
	protected Inner[] extracted() {
		return /*[*/new Inner[10]/*]*/;
	}
}
