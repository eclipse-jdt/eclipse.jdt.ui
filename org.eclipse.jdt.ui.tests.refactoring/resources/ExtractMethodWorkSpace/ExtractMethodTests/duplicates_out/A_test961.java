package duplicates_out;

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
