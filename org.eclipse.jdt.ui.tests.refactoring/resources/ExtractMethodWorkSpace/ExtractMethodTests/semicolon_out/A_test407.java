package semicolon_out;

public class A_test407 {
	public void foo() {
		boolean b;
		b= extracted();
	}

	protected boolean extracted() {
		return /*[*/10 == 20/*]*/;
	}
}