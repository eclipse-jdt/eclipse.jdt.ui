package p;
//private, nonstatic, final
class A{
	A(int i){
	}
	void f(){
		new A(1){
			void f(){
				x();
			}
		};
	}
	void x(){}
}