package p;
class A {
	private final class Inner extends B {
	}

	public static class B extends A {
		public void foo(){
		}
	}
	
	static B b = new B() {
		public void foo() {
			B b = new Inner();
		}
	};
}
