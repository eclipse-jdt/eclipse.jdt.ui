package duplicates_in;

// don't extract second occurence of
// 2 since it is in a inner class
public class A_test960 {
	private Object fO;

	public void foo() {
		/*[*/fO= new Object();/*]*/
	}
	public void bar() {
		foo();
		fO= new Object();
	}
}
