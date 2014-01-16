package defaultMethods_in;

public interface A_test5 {
	default int foo() {
		/*[*/return 0;/*]*/
	}
	
	static int foo() {
		return 0;
	}
}
