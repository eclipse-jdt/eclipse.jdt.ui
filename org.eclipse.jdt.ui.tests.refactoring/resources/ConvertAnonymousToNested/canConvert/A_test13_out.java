package p;
class A{
	private static final class Inner extends A {
		private Inner(Object s){
			super(s);
		}
	}
	A(Object s){}
	void f(){
		class Local{}
		new Inner(new Local());
	}
}