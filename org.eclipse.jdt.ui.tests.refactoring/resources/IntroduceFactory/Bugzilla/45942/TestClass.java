package p;

public class TestClass {
	public TestClass() {
	}
	
	public TestClass(int i) {
	}
	
	public static void main(String[] args) {
		TestClass t= /*[*/new TestClass()/*]*/;
		TestClass t1= new TestClass(10);
	}
}
