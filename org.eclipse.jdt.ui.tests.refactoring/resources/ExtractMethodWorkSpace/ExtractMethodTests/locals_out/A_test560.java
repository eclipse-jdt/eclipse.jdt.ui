package locals_out;

public class A_test560 {
	public boolean flag;
	public void foo() {
		int x= 0;
		while (x < 10) {
			x = extracted();
		}
	}
	protected int extracted() {
		int x;
		/*[*/x= 20;/*]*/
		return x;
	}
}

