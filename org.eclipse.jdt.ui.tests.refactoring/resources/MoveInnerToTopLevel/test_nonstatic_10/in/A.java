package p;

class A{
	A a;
	class Inner {
	}
	void f(A a){
		a.a.a.new Inner();
	}
}