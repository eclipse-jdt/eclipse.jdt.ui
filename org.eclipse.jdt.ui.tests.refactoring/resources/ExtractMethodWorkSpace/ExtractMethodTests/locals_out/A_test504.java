package locals_out;

public class A_test504 {
	public void foo() {
		int x= 10;
		double d= 10;
		
		/*]*/x= extracted(x, d);/*[*/
	}

	protected int extracted(int x, double d) {
		
		x= x + 10;
		x= g(d);
		
		return x;
	}
	
	private int g(double d) {
		return 10;
	}
}