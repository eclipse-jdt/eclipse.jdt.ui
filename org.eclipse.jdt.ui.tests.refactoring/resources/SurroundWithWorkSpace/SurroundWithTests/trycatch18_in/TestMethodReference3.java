package trycatch18_in;

import java.io.IOException;

public class TestMethodReference3 {
	{
		FI fi = /*[*/this::test/*]*/;
	}

	private void test() throws IOException {
	}

	interface FI {
		<T> void foo();
	}
}
