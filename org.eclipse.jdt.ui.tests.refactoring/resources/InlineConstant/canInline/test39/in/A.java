package p;

public class A<T> {
	static class B<U> {
		static int m= 0;

		static final int n= m;
	}

	static class C {
		void f() {
			int k= B.n;
		}
	}
}