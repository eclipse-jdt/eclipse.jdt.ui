package p;
//private, static, final
class A{
	private static final class Inner extends A{
		private Inner(int i){
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