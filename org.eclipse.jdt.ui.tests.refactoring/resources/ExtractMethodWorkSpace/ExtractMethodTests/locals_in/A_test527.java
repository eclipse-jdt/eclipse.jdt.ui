package locals_in;

public class A_test527 {
	public volatile boolean flag;
	
	protected void foo() {
		int i= 0;
		/*[*/try {
			if (flag)
				throwException();
			i= 10;
		} catch (Exception e) {
		}/*]*/
		read(i);
	}

	private void read(int i) {
	}
	
	private void throwException() throws Exception {
		throw new Exception();
	}
}
