package p;
class A {
	public static class B extends A {
		public void foo(){
		}
	}
	
	static B b = new B() {
		public void foo() {
			B b = new B(){};
		}
	};
}
