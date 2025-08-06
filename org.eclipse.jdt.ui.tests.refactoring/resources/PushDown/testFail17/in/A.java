package p;

class A {
	int f;
}
class B extends A {
	void m() {
		new A().f = 0;
	}
}