package p;

public class A<T> {
	static int m= 0;

	static class C {
		void f() {
			int k= A.m;
		}
	}
}