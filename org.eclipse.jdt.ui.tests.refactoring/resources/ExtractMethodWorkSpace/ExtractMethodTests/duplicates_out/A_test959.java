package duplicates_out;

// don't extract second occurence of
// 2 since it is in a inner class
public class A_test959 {
	public void foo() {
		int x= 10;
		int y= extracted(x);
		x= 20;
	}

	protected int extracted(int x) {
		return /*[*/x/*]*/;
	}
}
