package duplicates_in;

public class A_test986 {
	class A {
		int parentFoo;
	}

	class X extends A {
		int f() {
			return /*[*/parentFoo/*]*/;
		}

		void g() {
			int i;
			i= parentFoo;
		}
	}

}