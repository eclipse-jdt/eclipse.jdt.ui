package defaultMethods_in;

public class A_test2 {
	interface B {
		default int foo() {
			/*[*/return 0;/*]*/
		}
	}
}