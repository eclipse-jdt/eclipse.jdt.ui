package locals_out;

public class A_test502 {
	public void foo() {
		int x= 0;
		double d= 10;
		
		/*]*/extracted(x, d);/*[*/
	}

	protected void extracted(int x, double d) {
		
		int y= x + 10;
		g(d);
		
	}
	
	private int g(double d) {
		return 10;
	}
}