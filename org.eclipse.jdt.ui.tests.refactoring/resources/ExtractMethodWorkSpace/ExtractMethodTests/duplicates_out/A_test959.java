package duplicates_out;

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
