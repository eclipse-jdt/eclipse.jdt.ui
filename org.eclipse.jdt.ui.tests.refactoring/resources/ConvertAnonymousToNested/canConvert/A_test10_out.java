package p;
//private, static, final
class A{
	private static final class Inner extends A{
		private Inner(int i){
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
	static void x(){}
}