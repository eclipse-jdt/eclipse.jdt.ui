package p;

public class TestClass {
	private TestClass() {
	}
	
	public TestClass(int i) {
	}
	
	public static void main(String[] args) {
		TestClass t= createTestClass();
		TestClass t1= new TestClass(10);
	}

	public static TestClass createTestClass() {
		return new TestClass();
	}
}
