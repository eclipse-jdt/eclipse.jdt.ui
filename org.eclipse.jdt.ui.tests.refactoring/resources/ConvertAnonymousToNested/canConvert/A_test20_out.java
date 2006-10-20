package p;
class A {
	private final class Inner extends A {
		public void bar() {
				// TODO the return is misaligned
return;
		}
	}

	public void foo() {
		A foo= new Inner();
	}
}