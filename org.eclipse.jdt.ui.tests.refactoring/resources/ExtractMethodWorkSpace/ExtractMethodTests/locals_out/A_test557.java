package locals_out;

public class A_test557 {
	public boolean flag;
	public void foo() {
		int x= 0;
		for (int y= x; true;) {
			extracted();
		}
	}
	protected void extracted() {
		int x;
		/*[*/x= 20;/*]*/
	}
}

