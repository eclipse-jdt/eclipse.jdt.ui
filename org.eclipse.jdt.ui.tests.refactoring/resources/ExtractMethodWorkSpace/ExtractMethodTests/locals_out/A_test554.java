package locals_out;

public class A_test554 {
	public boolean flag;
	public void foo() {
		int x;
		extracted();
		x= 20;
	}
	protected void extracted() {
		int x;
		/*[*/if (flag)
			x= 10;/*]*/
	}
}

