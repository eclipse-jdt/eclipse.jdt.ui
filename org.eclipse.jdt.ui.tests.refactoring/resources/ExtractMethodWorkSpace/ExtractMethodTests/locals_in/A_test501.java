package locals_in;

public class A_test501 {
	public void foo() {
		int x= 0;
		double d= 10;
		
		/*]*/x= g(d);/*[*/
	}
	
	private int g(double d) {
		return 10;
	}
}