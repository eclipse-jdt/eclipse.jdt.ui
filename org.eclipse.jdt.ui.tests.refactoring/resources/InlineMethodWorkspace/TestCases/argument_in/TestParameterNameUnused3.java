package argument_in;

public class TestParameterNameUnused3 {
	public void main() {
		class T {
			int x;
		}
		/*]*/foo(10);/*[*/
	}
	
	public void foo(int x) {
		x= 20;
		bar(x);
	}
	
	public void bar(int z) {
	}
}
