package locals_out;

public class A_test575 {

	public void foo() {
		int i;
		extracted();
		if (true) {
			i= 10;
		} else {
		}
	}

	protected void extracted() {
		/*[*/int i= 0;/*]*/
	}
}

