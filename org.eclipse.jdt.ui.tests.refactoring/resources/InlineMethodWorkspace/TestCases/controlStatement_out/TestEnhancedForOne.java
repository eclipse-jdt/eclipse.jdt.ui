package controlStatement_out;

public class TestEnhancedForOne {
	public void main() {
		int[] array= null;
		for (int value: array)
			bar();/*[*/
	}
	
	public void foo() {
		bar();
	}
	public void bar() {
	}
}
