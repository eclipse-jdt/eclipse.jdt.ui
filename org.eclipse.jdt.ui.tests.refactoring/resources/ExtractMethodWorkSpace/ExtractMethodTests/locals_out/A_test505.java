package locals_out;

public class A_test505 {
	public void foo() {
		int x= 0;
		
		extracted(x);
	}

	protected void extracted(int x) {
		/*[*/x+= 1;/*]*/
	}
}