package try17_out;

class Foo4 extends Exception {}
class Bar4 extends Exception {}

public class A_test4 {

	void foo() throws Exception {
		extracted();
	}

	protected void extracted() throws Bar4, Foo4 {
		/*[*/try (Test4 t = new Test4()) {

		}/*]*/
	}
}

class Test4 implements AutoCloseable {
	Test4() throws Foo4 {

	}

	@Override
	public void close() throws Bar4 {
	}
}
