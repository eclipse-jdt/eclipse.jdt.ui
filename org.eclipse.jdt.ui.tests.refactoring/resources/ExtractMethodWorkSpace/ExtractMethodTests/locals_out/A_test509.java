package locals_out;

public class A_test509 {
	public void foo() {
		int x= 0;
		int y= 0;

		extracted(x);		
	}

	protected void extracted(int x) {
		int y;
		/*[*/y= x;
		x= 0;/*]*/
	}	
}