package p;

public class TestClass {
	public TestClass() {
	}
}

public class UseTestClass {
	public void foo() {
		/*[*/new TestClass()/*]*/;
	}
}
