package defaultMethods_in;

public interface A_test3 {
	interface B {
		default int foo() {
			/*[*/return 0;/*]*/
		}
	}
}