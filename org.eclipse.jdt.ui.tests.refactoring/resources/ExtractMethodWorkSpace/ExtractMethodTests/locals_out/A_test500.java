package locals_out;

public class A_test500 {
	public void foo() {
		int x= 0;
		double d= 10;
		
		/*]*/extracted(d);/*[*/
	}

	protected void extracted(double d) {
		g(d);
	}
	
	private int g(double d) {
		return 10;
	}
}