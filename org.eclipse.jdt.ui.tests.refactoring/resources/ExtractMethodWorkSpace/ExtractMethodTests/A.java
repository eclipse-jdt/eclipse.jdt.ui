public class A{
	public A() {
		this(5 + 6);
	}
	public A(int i) {
	}
	
	static class B extends A {
		public B() {
			super(5 + 7);
		}
	}
}
