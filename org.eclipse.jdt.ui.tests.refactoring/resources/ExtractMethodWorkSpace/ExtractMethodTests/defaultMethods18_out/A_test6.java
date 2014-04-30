package defaultMethods_in;

import java.io.IOException;

@FunctionalInterface
interface FI {
	int foo(int i) throws IOException;

	default FI method1(FI i1) {
		return extracted();
	}

	default FI extracted() {
		/*[*/return FI::fun;/*]*/
	}

	static int fun(int i) throws IOException {
		return i++;
	}
}