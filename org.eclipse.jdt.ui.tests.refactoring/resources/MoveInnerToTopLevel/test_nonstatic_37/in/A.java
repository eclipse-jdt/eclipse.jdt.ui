package p;
public class A {
	class Inner {
		public void doit() {
			foo(bar());
		}
	}
	public void foo(int value) {
	}
	public int bar() {
		return 0;
	}
}