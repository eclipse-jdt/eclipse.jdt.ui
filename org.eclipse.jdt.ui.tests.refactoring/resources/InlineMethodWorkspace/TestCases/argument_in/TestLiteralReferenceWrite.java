package argument_in;

public class TestLiteralReferenceWrite {
	public void main() {
		/*]*/foo(10);/*[*/
	}
	
	public void foo(int x) {
		x= 20;
		bar(x);
	}
	
	public void bar(int z) {
	}
}
