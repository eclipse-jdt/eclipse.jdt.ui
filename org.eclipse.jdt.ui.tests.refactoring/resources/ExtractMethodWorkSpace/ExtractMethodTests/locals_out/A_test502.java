package locals_out;

public class A_test502 {
	public void foo() {
		extracted();		
	}

	protected void extracted() {
		int x;
		int y;
		/*[*/x= 10;
		y= x;
		x= y;/*]*/
	}
}