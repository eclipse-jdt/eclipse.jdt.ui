package locals_out;

import java.io.IOException;

public class A_test528 {
	public void foo() throws IOException {
	}

	static class B extends A_test528 {
		public void bar() throws IOException {
			extracted();
		}

		protected void extracted() throws IOException {
			/*[*/super.foo();/*]*/
		}
	}
}