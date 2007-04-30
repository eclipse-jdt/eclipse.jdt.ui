package p;

public class TestVarArgsReordered {
	public static class FooParameter {
		public int[] is;
		public String[] a;
		public FooParameter(int[] is, String... a) {
			this.is = is;
			this.a = a;
		}
	}

	public void foo(FooParameter parameterObject) {

	}

	public void fooCaller() {
		foo(new TestVarArgsReordered.FooParameter(new int[0], new String[0]));
		foo(new TestVarArgsReordered.FooParameter(null, new String[0]));
		foo(new TestVarArgsReordered.FooParameter(new int[]{1, 2, 3, 4, 5}, new String[0]));
		foo(new TestVarArgsReordered.FooParameter(new int[]{Integer.parseInt("5")}, new String[0]));
		foo(new TestVarArgsReordered.FooParameter(new int[]{new Integer(6).intValue(), 2, 3}, new String[0]));
	}
}
