package return_out;

public class A_test725 {
	private boolean flag;
	protected void foo() {
		int i= 0;
		if (flag) {
			extracted();
			i= 20;
		} else {
			read(i);
		}
		read(i);
	}
	protected void extracted() {
		int i;
		/*[*/i= 10;/*]*/
	}
	private void read(int i) {
	}
}
