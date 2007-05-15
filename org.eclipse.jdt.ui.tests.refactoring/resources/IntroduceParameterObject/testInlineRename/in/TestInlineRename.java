package p;

public class TestInlineRename {
	int x = foo(1, 2, 3);
	public int foo(int xg, int yg, int zg) {
		return xg + yg;
	}
}

class B extends TestInlineRename {
	public int foo(int x, int y, int z) {
		System.out.println(x);
		foo(x, y, z);
		foo(x, z, y);
		super.foo(x, z, y);
		return super.x;
	}
}
