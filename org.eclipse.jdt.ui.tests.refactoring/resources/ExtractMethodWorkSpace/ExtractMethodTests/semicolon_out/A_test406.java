package semicolon_out;

public class A_test406 {
	public void foo() {
		if (extracted())
			foo();
	}

	protected boolean extracted() {
		return /*[*/1 == 10/*]*/;
	}
}