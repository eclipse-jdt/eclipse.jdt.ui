package locals_in;

import java.io.IOException;

public class A_test528 {
	public void foo() throws IOException {
	}

	static class B extends A_test528 {
		public void bar() throws IOException {
			/*[*/super.foo();/*]*/
		}
	}
}