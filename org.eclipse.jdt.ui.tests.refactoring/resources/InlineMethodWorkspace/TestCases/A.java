public class A {
	class Inner {
		public void bar() {
			foo();
		}
	}
	public void foo() {
		B b= new B();
		b.foo();
	}
}
