package lambdaExpression18_in;

import java.io.IOException;

@FunctionalInterface
interface FI {
	int foo(int i) throws IOException;

	default FI method(FI i1) throws InterruptedException {
		/*[*/if (i1 == null)
			throw new InterruptedException();
		return x -> {
			throw new IOException();
		};/*]*/
	}
}