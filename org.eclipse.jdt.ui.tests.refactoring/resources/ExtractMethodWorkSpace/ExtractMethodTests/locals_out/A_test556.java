package locals_out;

public class A_test556 {
	public boolean flag;
	public void foo() {
		int x= 0;
		while (true) {
			for (int y= 0; true; y= x) {
				x = extracted();
			}
		}
	}
	protected int extracted() {
		int x;
		/*[*/x= 20;/*]*/
		return x;
	}
}

