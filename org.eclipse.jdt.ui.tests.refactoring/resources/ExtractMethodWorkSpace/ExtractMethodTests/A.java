public class A {
	public void foo() {
		bar(baz(), baz());
	}
	public void bar(int i, int y) {
	}
	public int baz() {
		return 0;
	}
}

