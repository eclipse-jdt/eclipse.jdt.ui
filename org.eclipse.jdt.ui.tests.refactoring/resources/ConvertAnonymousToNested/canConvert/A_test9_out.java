package p;
//private, nonstatic, final
class A{
	private final class Inner extends A {
		private Inner(int i) {
			super(i);
		}
		void f(){
			y= 0;
		}
	}
	int y;
	A(int i){
	}
	void f(){
		new Inner(1);
	}
}