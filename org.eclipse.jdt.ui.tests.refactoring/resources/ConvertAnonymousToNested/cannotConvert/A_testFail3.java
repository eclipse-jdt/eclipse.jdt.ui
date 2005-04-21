package p;
class A {
	//duplicate
}
public class A {
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
