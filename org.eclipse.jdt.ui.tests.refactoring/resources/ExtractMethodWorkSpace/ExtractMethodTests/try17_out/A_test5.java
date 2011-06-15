package try_out;

class Foo extends Exception {}
class Bar extends Exception {}

public class A_test5 {

	void foo() throws Exception {
		extracted();
	}

	protected void extracted() throws Bar {
		/*[*/try (Test t = new Test()) {

		}/*]*/
	}
}

class Test implements AutoCloseable {
	@Override
	public void close() throws Bar {
	}
}
