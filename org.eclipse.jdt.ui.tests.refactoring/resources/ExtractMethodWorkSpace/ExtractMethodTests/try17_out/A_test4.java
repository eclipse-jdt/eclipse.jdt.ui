package try_out;

class Foo extends Exception {}
class Bar extends Exception {}

public class A_test4 {

	void foo() throws Exception {
		extracted();
	}

	protected void extracted() throws Bar, Foo {
		/*[*/try (Test t = new Test()) {

		}/*]*/
	}
}

class Test implements AutoCloseable {
	Test() throws Foo {

	}

	@Override
	public void close() throws Bar {
	}
}
