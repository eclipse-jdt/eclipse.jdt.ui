package simple18_in;

import java.util.function.Supplier;

public class TestLambda1 {
	public static Supplier<Integer> add(int a, int b) {
		Supplier<Integer> sum= /*]*/m(a, b)/*[*/;
		return sum;
	}

	private static Supplier<Integer> m(int x, int y) {
		Supplier<Integer> supplier= () -> { return x + y; };
		return supplier;
	}
}
