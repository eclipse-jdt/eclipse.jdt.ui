package locals_in;

public class A_test503 {
	public void foo() {
		int x= 10;
		double d= 10;
		
		/*]*/
		x++;
		x= g(d);
		/*[*/
	}
	
	private int g(double d) {
		return 10;
	}
}