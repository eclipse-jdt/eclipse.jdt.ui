package invalid;

public class TestRecursion {
	public void main() {
		/*]*/foo();/*[*/
	}
	
	public void foo() {
		foo();
	}
}
