package expression_out;

public class A_test615 {
	class Inner {
	}
	public void foo() {
		Inner inner= null;
		boolean b;
		b= extracted(inner);
	}
	protected boolean extracted(Inner inner) {
		return /*[*/inner instanceof Inner/*]*/;
	}
}
