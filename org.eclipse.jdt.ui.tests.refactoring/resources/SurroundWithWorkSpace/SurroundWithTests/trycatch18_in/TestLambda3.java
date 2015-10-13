package trycatch18_in;

import java.io.FileNotFoundException;

interface TestLambda2 {
	int foo(int i);

	default TestLambda2 method1() {
		return x -> {
			if (x == 0)
				/*[*/throw new FileNotFoundException();/*]*/
			return x;
		};
	}
}