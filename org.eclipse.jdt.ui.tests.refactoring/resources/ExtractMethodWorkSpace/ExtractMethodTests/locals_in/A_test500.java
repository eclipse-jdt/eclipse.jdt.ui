package locals_in;

public class A_test500 {
	public void foo() {
		int x= 0;
		double d= 10;
		
		/*]*/g(d);/*[*/
	}
	
	private int g(double d) {
		return 10;
	}
}