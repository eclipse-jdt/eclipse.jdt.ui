package p;

public class TestInlineRename {
	int x = foo(new FooParameter(1, 2), 3);
	public static class FooParameter {
		public int xg;
		public int yg;
		public FooParameter(int xg, int yg) {
			this.xg = xg;
			this.yg = yg;
		}
	}
	public int foo(FooParameter parameterObject, int zg) {
		return parameterObject.xg + parameterObject.yg;
	}
}

class B extends TestInlineRename {
	public int foo(FooParameter parameterObject, int z) {
		System.out.println(parameterObject.xg);
		foo(new FooParameter(parameterObject.xg, parameterObject.yg), z);
		foo(new FooParameter(parameterObject.xg, z), parameterObject.yg);
		super.foo(new FooParameter(parameterObject.xg, z), parameterObject.yg);
		return super.x;
	}
}
