package trycatch18_out;

import java.io.FileNotFoundException;

interface TestLambda2 {
	int foo(int i);

	default TestLambda2 method1() {
		return x -> {
			if (x == 0)
				try {
					/*[*/throw new FileNotFoundException();/*]*/
				} catch (FileNotFoundException e) {
				}
			return x;
		};
	}
}