package locals_out;

public class A_test521 {
	public volatile boolean flag;
	
	public void foo() {
		int i= 5;
		i = extracted(i);
		i--;
	}

	protected int extracted(int i) {
		/*[*/if (flag)
			i= 10;/*]*/
		return i;
	}
}

