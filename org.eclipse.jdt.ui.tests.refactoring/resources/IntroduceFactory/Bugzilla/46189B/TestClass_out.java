package p;

public class TestClass {
	public TestClass createTestClass() {
		return new TestClass();
	}
	private TestClass() {
	}
}

public class UseTestClass {
	public void foo() {
		createTestClass();
	}
}
