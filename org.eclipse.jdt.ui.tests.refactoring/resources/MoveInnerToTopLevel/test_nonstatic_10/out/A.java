package p;

class A{
	A a;
	class Inner {
	}
	void f(A a){
		new Inner(a.a.a);
	}
}