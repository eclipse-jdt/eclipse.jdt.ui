package locals_out;

public class A_test563 {
	public boolean flag;
	public void foo() {
		int x= 0;
		do {
			int y= x;
			extracted();
		} while ((x= 20) < 10);
	}
	protected void extracted() {
		int x;
		/*[*/x= 20;/*]*/
	}
}

