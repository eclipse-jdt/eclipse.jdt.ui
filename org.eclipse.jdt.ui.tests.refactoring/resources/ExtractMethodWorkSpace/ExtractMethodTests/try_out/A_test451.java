package try_out;

public class A_test451 {
	public void foo() {
		extracted();
	}

	protected void extracted() {
		/*[*/try {
			g();
		} catch (java.io.IOException e) {
		}/*]*/
	}

	public void g() throws java.io.IOException {
	}
}
