package p;
//private, nonstatic, final
class A{
	int y;
	A(int i){
	}
	void f(){
		new A(1){
			void f(){
				y= 0;
			}
		};
	}
}