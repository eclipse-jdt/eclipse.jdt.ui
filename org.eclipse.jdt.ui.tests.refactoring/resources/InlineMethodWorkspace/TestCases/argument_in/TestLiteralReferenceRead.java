package argument_in;

public class TestLiteralReferenceRead {
	public void main() {
		/*]*/foo(10);/*[*/
	}
	
	public void foo(int x) {
		int i= x;
		bar(x);
	}
	
	public void bar(int z) {
	}
}
