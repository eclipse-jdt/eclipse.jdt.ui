package p;
//private, static, final
class A{
	private final class Inner extends A {
		int X = 0;
		private Inner(int i) {
			super(i);
		}
		void f(){
		}
	}
	A(int i){
	}
	void f(){
		new Inner(1);
	}
}