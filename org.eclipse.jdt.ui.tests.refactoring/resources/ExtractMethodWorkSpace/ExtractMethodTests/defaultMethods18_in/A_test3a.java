package defaultMethods_in;

public interface A_test3a {
	interface B {
		public default int foo() {
			/*[*/return 0;/*]*/
		}
	}
}