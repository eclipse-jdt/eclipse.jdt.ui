package locals_out;

public class A_test576 {
	public void foo() {
		int avail;
		extracted();
		for (;;) {
			try {
			} finally {
				avail= 20;
			}
		}
	}

	protected void extracted() {
		/*[*/int avail= 10;/*]*/
	}
}

