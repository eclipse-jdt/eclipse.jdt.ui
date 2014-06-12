package p1;

public class A<T> {
	int i;

	class C {
		B b = null;
		void m() { // move to b
			i = 0;
		}
	}
}
