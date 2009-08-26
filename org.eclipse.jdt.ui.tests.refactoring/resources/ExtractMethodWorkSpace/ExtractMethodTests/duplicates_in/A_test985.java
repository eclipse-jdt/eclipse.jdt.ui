package duplicates_in;

public class A_test985 {
	class A {
		int parentFoo;
	}

	class X extends A {
		int f() {
			return /*[*/parentFoo/*]*/;
		}

		void g() {
			super.parentFoo= 1;
		}
	}

}