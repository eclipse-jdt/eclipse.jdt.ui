class A<T> {
	static int CONST;

	class C {
		B b = null;
		void m() { // move to b
			CONST = 0;
		}
	}
}
class B {
}