package return_in;

public class A_test725 {
	private boolean flag;
	protected void foo() {
		int i= 0;
		if (flag) {
			/*[*/i= 10;/*]*/
			i= 20;
		} else {
			read(i);
		}
		read(i);
	}
	private void read(int i) {
	}
}
