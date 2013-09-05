package destination_in;

public class A_test1061 {
	static class B {
		class C {
			int foo() {
				return extracted();
			}
		}

		protected int extracted() {
			/*[*/return 0;/*]*/
		}
	}
}