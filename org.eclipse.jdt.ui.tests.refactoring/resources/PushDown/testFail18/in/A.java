package p;

class A {
	int f;
}
class B extends A {
	void m() {
		A a = new A();
		a.f = 5;
	}
}