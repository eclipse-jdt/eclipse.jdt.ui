package argument_in;

public class TestParameterNameUsed1 {
	public void main() {
		int x= 20;
		/*]*/foo(10);/*[*/
	}
	
	public void foo(int x) {
		x= 20;
		bar(x);
	}
	
	public void bar(int z) {
	}
}
