package trycatch_out;

import java.io.IOException;

public class TestSuperCall {
	public void foo() throws IOException {
	}
	
	static class A extends TestSuperCall {
		public void bar() {
			try {
				/*[*/super.foo();/*]*/
			} catch (IOException e) {
			}
		}
	}
}

