package tryresources18_in;

import java.io.FileNotFoundException;

interface TestLambda3 {
	int foo(int i);

	default TestLambda3 method1() {
		return x -> {
			if (x == 0)
				/*[*/throw new FileNotFoundException();/*]*/
			return x;
		};
	}
}