package duplicates_out;

public class A_test960 {
	private Object fO;

	public void foo() {
		extracted();
	}
	protected void extracted() {
		/*[*/fO= new Object();/*]*/
	}
	public void bar() {
		foo();
		extracted();
	}
}
