package controlStatement_out;

public class TestEnhancedForTwo {
	public void main() {
		int[] array= null;
		for (int value: array) {
			bar();
			bar();
		}/*[*/
	}
	
	public void foo() {
		bar();
		bar();
	}
	public void bar() {
	}
}
