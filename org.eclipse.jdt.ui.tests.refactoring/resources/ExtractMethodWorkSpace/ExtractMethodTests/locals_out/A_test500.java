package locals_out;

public class A_test500 {
	public void foo() {
		int x= 10;
		
		extracted(x);
	}

	protected void extracted(int x) {
		/*[*/int y= x;/*]*/
	}
}