package locals_in;

public class A_test504 {
	public void foo() {
		int x= 10;
		double d= 10;
		
		/*]*/
		x= x + 10;
		x= g(d);
		/*[*/
	}
	
	private int g(double d) {
		return 10;
	}
}