package p;

class A {
	void m() {}
}

class Ref {
	void bar(A a) {
		a.m();
	}
}
