package p;

public class TestRecursiveSimple {
	public void foo(int x, int y) {
		if (x < 0)
			foo(x, y--);
		foo(x, y);
	}
}
