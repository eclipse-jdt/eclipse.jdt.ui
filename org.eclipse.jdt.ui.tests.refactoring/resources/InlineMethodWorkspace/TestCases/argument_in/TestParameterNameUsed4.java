package argument_in;

public class TestParameterNameUsed4 {
	public void main() {
		class x {}
		/*]*/foo(10);/*[*/
	}
	
	public void foo(int x) {
		x= 20;
		bar(x);
	}
	
	public void bar(int z) {
	}
}
