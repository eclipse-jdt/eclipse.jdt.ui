package controlStatement_in;

public class TestEnhancedForTwo {
	public void main() {
		int[] array= null;
		for (int value: array)
			/*]*/foo();/*[*/
	}
	
	public void foo() {
		bar();
		bar();
	}
	public void bar() {
	}
}
