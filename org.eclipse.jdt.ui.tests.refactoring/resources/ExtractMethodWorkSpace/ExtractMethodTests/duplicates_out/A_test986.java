package duplicates_out;

public class A_test986 {
	class A {
		int parentFoo;
	}

	class X extends A {
		int f() {
			return extracted();
		}

		protected int extracted() {
			return /*[*/parentFoo/*]*/;
		}

		void g() {
			int i;
			i= extracted();
		}
	}

}