package argument_in;

public class TestLocalReferenceUnused {
	public void main() {
		int i= 10;
		/*]*/foo(i);/*[*/
		int x= 7;
	}
	
	public void foo(int x) {
		x= x + 10;
		bar(x);
	}
	
	public void bar(int z) {
	}
}
