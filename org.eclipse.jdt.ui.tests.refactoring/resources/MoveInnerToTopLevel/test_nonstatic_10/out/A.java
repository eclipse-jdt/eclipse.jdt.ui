package p;

class A{
	A a;
	void f(A a){
		new Inner(a.a.a);
	}
}