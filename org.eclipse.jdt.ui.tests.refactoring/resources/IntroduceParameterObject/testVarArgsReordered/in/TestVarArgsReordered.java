package p;

public class TestVarArgsReordered {
	public void foo(String[] a, int... is) {

	}

	public void fooCaller() {
		foo(new String[0]);
		foo(new String[0], null);
		foo(new String[0], new int[] {1,2,3,4});
		foo(new String[0], 1, 2, 3, 4, 5);
		foo(new String[0], Integer.parseInt("5"));
		foo(new String[0], new Integer(6).intValue(), 2, 3);
	}
}
