package tryresources18_out;

import java.io.FileNotFoundException;

interface TestLambda3 {
	int foo(int i);

	default TestLambda3 method1() {
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