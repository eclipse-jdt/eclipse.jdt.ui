package p;

public class TestRecursiveSimpleReordered {
	public void foo(int x, int y) {
		if (x < 0)
			foo(x, y);
		foo(y, x);
	}
}
