package p;
//private, nonstatic, final
class A{
	private final class Inner extends A {
		private Inner(int i) {
			super(i);
		}
		void f(){
			x();
		}
	}
	A(int i){
	}
	void f(){
		new Inner(1);
	}
	void x(){}
}