package defaultMethods_in;

public class A_test2a {
	interface B {
		public default int foo() {
			/*[*/return 0;/*]*/
		}
	}
}