package argument_in;

public class TestLocalReference {
	public void main() {
		int i= 10;
		/*]*/foo(i);/*[*/
	}
	
	public void foo(int x) {
		x= x + 10;
		bar(x);
	}
	
	public void bar(int z) {
	}
}
