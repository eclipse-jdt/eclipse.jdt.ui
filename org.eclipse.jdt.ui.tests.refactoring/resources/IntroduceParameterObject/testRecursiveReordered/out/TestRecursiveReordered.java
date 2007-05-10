package p;

public class TestRecursiveReordered {
	public static class FooParameter {
		public int y;
		public int x;
		public FooParameter(int y, int x) {
			this.y = y;
			this.x = x;
		}
	}

	public void foo(FooParameter parameterObject) {
		int y = parameterObject.y;
		if (parameterObject.x < 0)
			foo(new FooParameter(y--, parameterObject.x));
		foo(new FooParameter(parameterObject.x, y));
	}
}
