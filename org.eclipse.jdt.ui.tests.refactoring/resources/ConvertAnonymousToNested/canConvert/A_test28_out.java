package p;
public class A {
	private final class Inner extends A {
	}

	public static class B extends A {
		public void foo(){
		}
	}
	
	static A a = new B() {
		public void foo() {
			A a = new Inner();
		}
	};
}
