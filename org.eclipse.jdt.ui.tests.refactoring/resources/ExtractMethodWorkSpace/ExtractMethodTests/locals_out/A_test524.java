package locals_out;

public class A_test524 {
	public volatile boolean flag;
	
	protected void foo() {
		int i= 0;
		try {
			i = extracted();
		} catch (Exception e) {
		}
		read(i);
	}

	protected int extracted() throws Exception {
		int i;
		/*[*/if (flag)
			throw new Exception();
		i= 10;/*]*/
		return i;
	}

	private void read(int i) {
	}
}
