package staticMethods_in;

interface A_test102 {
	class B {
		static {
			extracted();
		}
	}

	static void extracted() {
		/*[*/int i= 0;/*]*/
	}
}