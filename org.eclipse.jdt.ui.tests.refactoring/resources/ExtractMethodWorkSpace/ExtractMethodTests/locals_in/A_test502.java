package locals_in;

public class A_test502 {
	public void foo() {
		int x= 0;
		double d= 10;
		
		/*]*/
		int y= x + 10;
		g(d);
		/*[*/
	}
	
	private int g(double d) {
		return 10;
	}
}