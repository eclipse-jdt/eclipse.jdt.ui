package argument_out;

public class TestLocalReference {
	public void main() {
		int i= 10;
		/*]*/i= i + 10;
		bar(i);/*[*/
	}
	
	public void foo(int x) {
		x= x + 10;
		bar(x);
	}
	
	public void bar(int z) {
	}
}
