package locals_out;

public class A_test561 {
	public boolean flag;
	public void foo() {
		int x= 0;
		do {
			x = extracted();
		} while (x < 10);
	}
	protected int extracted() {
		int x;
		/*[*/x= 20;/*]*/
		return x;
	}
}

