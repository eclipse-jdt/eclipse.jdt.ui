package duplicates_out;

// don't extract second occurence of
// 2 since it is in a inner class
public class A_test961 {
	private Object fO;

	public void foo(Object o) {
		extracted(o);
	}
	protected void extracted(Object o) {
		/*[*/fO= o;/*]*/
	}
	public void bar(Object x) {
		foo(x);
		extracted(x);
	}
}
