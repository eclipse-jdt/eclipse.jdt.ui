package locals_out;

public class A_test527 {
	public volatile boolean flag;
	
	protected void foo() {
		int i= 0;
		i = extracted(i);
		read(i);
	}

	protected int extracted(int i) {
		/*[*/try {
			if (flag)
				throwException();
			i= 10;
		} catch (Exception e) {
		}/*]*/
		return i;
	}

	private void read(int i) {
	}
	
	private void throwException() throws Exception {
		throw new Exception();
	}
}
