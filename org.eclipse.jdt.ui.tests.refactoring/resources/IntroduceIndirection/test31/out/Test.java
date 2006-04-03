package p;

public class Test {
    protected int foo() {
            return 1;
    }
    int useFoo() {
            return TestO.foo(this);
    }
}
class TestO extends Test {
    /**
	 * @param test
	 * @return
	 */
	public static int foo(Test test) {
		return test.foo();
	}

	int useFoo() {
            return TestO.foo(this);
    }
}