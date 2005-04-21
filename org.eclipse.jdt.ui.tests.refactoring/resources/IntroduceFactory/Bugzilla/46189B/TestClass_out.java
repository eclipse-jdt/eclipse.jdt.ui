package p;

public class TestClass {
	public static TestClass createTestClass() {
		return new TestClass();
	}

	private TestClass() {
	}
}

public class UseTestClass {
	public void foo() {
		/*[*/TestClass.createTestClass();
	}
}
