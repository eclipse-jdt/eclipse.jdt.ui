package trycatch_in;

import java.io.IOException;

public class TestSuperCall {
	public void foo() throws IOException {
	}
	
	static class A extends TestSuperCall {
		public void bar() {
			/*[*/super.foo();/*]*/
		}
	}
}

