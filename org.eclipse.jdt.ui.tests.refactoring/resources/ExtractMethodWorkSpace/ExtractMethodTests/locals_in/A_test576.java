package locals_in;

public class A_test576 {
	public void foo() {
		/*[*/int avail= 10;/*]*/
		for (;;) {
			try {
			} finally {
				avail= 20;
			}
		}
	}
}

