package p;
class A {
	private static final class Inner implements I {
		public void foo(){
			
		}
	}
	interface I{
		void foo();
	}
	static I i = new I(){
		public void foo(){
			I i = new Inner();
		}
	};
}
