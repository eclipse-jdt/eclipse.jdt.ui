package locals_out;

public class A_test503 {
	public void foo() {
		int x= 10;
		
		extracted(x);
	}

	protected void extracted(int x) {
		/*[*/x++;/*]*/
	}
}