package p;

class A{
	void f(){
		new Inner(this){
		};
	}
}