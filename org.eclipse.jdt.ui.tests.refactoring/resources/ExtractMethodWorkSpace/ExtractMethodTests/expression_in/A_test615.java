package expression_in;

public class A_test615 {
	class Inner {
	}
	public void foo() {
		Inner inner= null;
		boolean b;
		b= /*[*/inner instanceof Inner/*]*/;
	}
}
