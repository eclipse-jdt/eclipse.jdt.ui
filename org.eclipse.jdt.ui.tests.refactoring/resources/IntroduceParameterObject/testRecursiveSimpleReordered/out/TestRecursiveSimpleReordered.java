package p;

public class TestRecursiveSimpleReordered {
	public static class FooParameter {
		public int x;
		public int y;
		public FooParameter(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	public void foo(FooParameter parameterObject) {
		if (parameterObject.x < 0)
			foo(parameterObject);
		foo(new FooParameter(parameterObject.y, parameterObject.x));
	}
}
