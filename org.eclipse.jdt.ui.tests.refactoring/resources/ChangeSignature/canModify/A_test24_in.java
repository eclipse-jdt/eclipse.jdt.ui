package p;

class A {
	A(int a, int b){}
	
	void f(){
		new A(1, 4){
		};
	}
}