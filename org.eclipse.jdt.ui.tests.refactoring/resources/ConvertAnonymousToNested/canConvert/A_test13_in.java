package p;
class A{
	A(Object s){}
	void f(){
		class Local{}
		new A(new Local()){
		};
	}
}