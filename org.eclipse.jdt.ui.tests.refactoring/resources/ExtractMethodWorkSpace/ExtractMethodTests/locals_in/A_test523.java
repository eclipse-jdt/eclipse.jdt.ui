package locals_in;

public class A_test523 {
	public volatile boolean flag;

	protected void foo() {
		int i= 0;
		/*[*/try {
			if (flag)
				throw new Exception();
			i= 10;
		} catch (Exception e) {
		}/*]*/
		read(i);
	}

	private void read(int i) {
	}
}
