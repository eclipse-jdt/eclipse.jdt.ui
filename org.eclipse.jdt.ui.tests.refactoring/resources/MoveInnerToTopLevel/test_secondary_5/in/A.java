package p;

class A{
	A a;
	class Inner {
	}
}

class Secondary {
	void f(A a){
		a.a.a.new Inner();
	}
}