package p; //6, 14 - 6, 21

class A {
	void f() {
		class D{}
		D x= new D();
		D d= x;
	}
}