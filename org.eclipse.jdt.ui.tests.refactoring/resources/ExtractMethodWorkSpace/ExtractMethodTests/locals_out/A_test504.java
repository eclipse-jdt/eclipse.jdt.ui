package locals_out;

public class A_test504 {
	public void foo() {
		int x= 10;
		
		extracted(x);
	}

	protected void extracted(int x) {
		/*[*/--x;/*]*/
	}
}