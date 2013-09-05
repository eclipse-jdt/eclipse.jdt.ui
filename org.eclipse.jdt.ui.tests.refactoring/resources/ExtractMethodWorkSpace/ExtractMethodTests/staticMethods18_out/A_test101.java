package staticMethods_out;

public interface A_test101 {
	class B {
		int foo() {
			return extracted();
		}
	}

	static int extracted() {
		/*[*/return 0;/*]*/
	}
}