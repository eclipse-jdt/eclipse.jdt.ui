package p;
//private, static, final
class A{
	A(int i){
	}
	void f(){
		new A(1){
			int X = 0;
			void f(){
			}
		};
	}
}