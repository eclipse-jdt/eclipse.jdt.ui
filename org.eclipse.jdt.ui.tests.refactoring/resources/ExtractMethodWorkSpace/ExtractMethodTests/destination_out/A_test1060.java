package destination_in;

public class A_test1060 {
	interface B {
		class C {
			int foo() {
				return extracted();
			}
		}

		static int extracted() {
			/*[*/return 0;/*]*/
		}
	}
}