package p;
class A {
	private static final class Inner extends A {
		private Inner(Object x) {
			super(x);
		}
	}
	A(Object x){
	}
	void f(){
		new Inner(this);
	}
}