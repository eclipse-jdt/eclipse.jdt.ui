package controlStatement_in;

public class TestEnhancedForOne {
	public void main() {
		int[] array= null;
		for (int value: array)
			/*]*/foo();/*[*/
	}
	
	public void foo() {
		bar();
	}
	public void bar() {
	}
}
