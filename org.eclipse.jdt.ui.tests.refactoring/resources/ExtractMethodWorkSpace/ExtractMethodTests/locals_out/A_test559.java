package locals_out;

public class A_test559 {
	public boolean flag;
	public void foo() {
		int x= 0;
		for (int y= 0; x < 10; x= 20) {
			/*]*/extracted();/*[*/
		}
	}
	protected void extracted() {
		int x;
		x= 20;
	}
}

