package staticMethods_in;

interface A_test106 {
	enum E {
		A;
		int foo() {
			return extracted();
		}
	}

	static int extracted() {
		/*[*/return 0;/*]*/
	}
}