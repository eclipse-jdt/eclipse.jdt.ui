package locals_out;

public class A_test501 {
	public void foo() {
		int x= 0;
		double d= 10;
		
		/*]*/x= extracted(d);/*[*/
	}

	protected int extracted(double d) {
		int x;
		x= g(d);
		return x;
	}
	
	private int g(double d) {
		return 10;
	}
}