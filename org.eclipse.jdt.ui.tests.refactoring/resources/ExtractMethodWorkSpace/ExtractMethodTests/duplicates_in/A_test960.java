package duplicates_in;

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
