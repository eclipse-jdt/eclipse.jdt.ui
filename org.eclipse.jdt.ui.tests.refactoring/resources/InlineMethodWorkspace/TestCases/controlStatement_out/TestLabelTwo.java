package controlStatement_in;

public class TestEnhancedForOne {
	public void main() {
		int[] array= null;
		aLabel: /*]*/{
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
