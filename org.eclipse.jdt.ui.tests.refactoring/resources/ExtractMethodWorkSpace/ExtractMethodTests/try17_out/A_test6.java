package try17_out;

class Foo6 extends Exception {}
class Bar6 extends Exception {}

public class A_test6 {

	void foo() throws Exception {
		extracted();
	}

	protected void extracted() throws Foo6 {
		/*[*/try (Test6 t = new Test6()) {

		}/*]*/
	}
}

class Test6 implements AutoCloseable {
	Test6() throws Foo6 {

	}

	@Override
	public void close() {
	}
}
