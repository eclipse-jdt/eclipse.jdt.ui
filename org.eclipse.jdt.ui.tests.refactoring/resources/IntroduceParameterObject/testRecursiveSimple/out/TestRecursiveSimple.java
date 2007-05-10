package p;

public class TestRecursiveSimple {
	public static class FooParameter {
		public int x;
		public int y;
		public FooParameter(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	public void foo(FooParameter parameterObject) {
		int y = parameterObject.y;
		if (parameterObject.x < 0)
			foo(new FooParameter(parameterObject.x, y--));
		foo(new FooParameter(parameterObject.x, y));
	}
}
