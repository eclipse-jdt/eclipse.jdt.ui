package locals_in;

public class A_test526 {
	private static class Exception1 extends Exception {
	}
	private static class Exception2 extends Exception {
	}
	
	public volatile boolean flag;
	
	protected void foo() {
		int i= 10;
		/*[*/try {
			try {
				if (flag)
					throw new Exception1();
				if (!flag)
					throw new Exception2();
			} catch (Exception1 e) {
			}
			i= 10;
		} catch (Exception2 e) {
		}/*]*/
		read(i);
	}

	private void read(int i) {
	}
}
