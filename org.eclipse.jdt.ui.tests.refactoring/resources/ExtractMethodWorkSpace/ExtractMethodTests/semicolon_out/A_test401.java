package semicolon_out;

public class A_test401 {
	public void foo() {
		extracted();
	}

	protected void extracted() {
		/*[*/foo();/*]*/
	} 
}
