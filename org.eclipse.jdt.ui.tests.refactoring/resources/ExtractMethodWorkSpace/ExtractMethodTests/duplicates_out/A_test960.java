package duplicates_out;

// don't extract second occurence of
// 2 since it is in a inner class
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
