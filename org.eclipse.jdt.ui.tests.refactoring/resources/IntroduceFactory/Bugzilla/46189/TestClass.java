package p;

public class TestClass {
	public TestClass() {
	}
}

class UseTestClass {
	public void foo() {
		/*[*/new TestClass()/*]*/;
	}
}
