package tryresources18_in;

import java.io.FileNotFoundException;

interface TestLambda4 {
	int foo(int i);

	default TestLambda4 method1() {
		/*[*/return x -> {
			if (x == 0)
				throw new FileNotFoundException();
			return x;
		};/*]*/
	}
}