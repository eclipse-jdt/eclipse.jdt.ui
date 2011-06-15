package try_in;

class Foo extends Exception {}
class Bar extends Exception {}

public class A_test4 {

	void foo() throws Exception {
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
