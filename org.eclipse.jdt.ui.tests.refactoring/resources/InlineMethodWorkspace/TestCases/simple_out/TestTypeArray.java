package simple_out;

public class TestTypeArray {
	public void main() {
		TestTypeArray[][] x = null;
		x= new TestTypeArray[10][];
	}
	
	public void foo(TestTypeArray[][] x) {
		x= new TestTypeArray[10][];
	}
}
