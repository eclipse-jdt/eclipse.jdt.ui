package semicolon_out;

public class A_test410 {
	public void foo() {
		extracted();
	}

	protected void extracted() {
		/*[*/switch (10) {
			case 1:
				foo();
		}/*]*/
	}
}