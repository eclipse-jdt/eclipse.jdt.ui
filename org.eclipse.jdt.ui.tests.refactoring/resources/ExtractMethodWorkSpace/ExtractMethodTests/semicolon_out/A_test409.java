package semicolon_out;

public class A_test409 {
	public void foo() {
		extracted();
	}

	protected void extracted() {
		/*[*/synchronized (this) {
			foo();
		}/*]*/
	}
}