class A<T> {
	static int CONST;

	class C {
		B b = null;
	}
}
class B {

	void m() { // move to b
		A.CONST = 0;
	}
}