package semicolon_out;

public class A_test404 {
	public void foo() {
		int x;
		extracted();
		x= 10;
	}

	protected void extracted() {
		/*[*/int x= 0;/*]*/
	}
}