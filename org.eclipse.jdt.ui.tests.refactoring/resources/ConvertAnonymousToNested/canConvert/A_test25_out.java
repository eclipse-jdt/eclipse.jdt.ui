package p;
class A {
	private final class Inner implements I {
		public void foo(){
			
		}
	}
	interface I{
		void foo();
	}
	static A i = new A(){
		public void foo(){
			I i = new Inner();
		}
	};
}
