package p;

public class TestRecursiveReordered {
	public void foo(int x, int y) {
		if (x < 0)
			foo(x, y--);
		foo(y, x);
	}
}
