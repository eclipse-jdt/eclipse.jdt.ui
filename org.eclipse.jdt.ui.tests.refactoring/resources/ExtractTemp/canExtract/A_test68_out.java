package p; //6, 14 - 6, 21

class A {
	void f() {
		class D{}
		D temp= new D();
		D d= temp;
	}
}