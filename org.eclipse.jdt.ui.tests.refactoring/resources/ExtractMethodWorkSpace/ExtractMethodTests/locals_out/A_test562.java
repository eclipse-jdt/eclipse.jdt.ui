package locals_out;

public class A_test562 {
	public boolean flag;
	public void foo() {
		int x= 0;
		do {
			int y= x;
			x = extracted();
		} while (true);
	}
	protected int extracted() {
		int x;
		/*[*/x= 20;/*]*/
		return x;
	}
}

