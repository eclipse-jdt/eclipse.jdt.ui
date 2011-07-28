package try17_in;

class Foo4 extends Exception {}
class Bar4 extends Exception {}

public class A_test4 {

	void foo() throws Exception {
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
