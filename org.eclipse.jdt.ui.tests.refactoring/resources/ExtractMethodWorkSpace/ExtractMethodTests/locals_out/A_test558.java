package locals_out;

public class A_test558 {
	public boolean flag;
	public void foo() {
		int x= 0;
		for (int y= 0; (x= 20) < 10; y= x) {
			x = extracted();
		}
	}
	protected int extracted() {
		int x;
		/*[*/x= 20;/*]*/
		return x;
	}
}

