package locals_out;

public class A_test513 {
	public void foo() {
		int y;
		int x = extracted();
		x++;
		y= 10;
	}

	protected int extracted() {
		/*[*/int x= 0;
		int y= 0;/*]*/
		return x;
	}
}
