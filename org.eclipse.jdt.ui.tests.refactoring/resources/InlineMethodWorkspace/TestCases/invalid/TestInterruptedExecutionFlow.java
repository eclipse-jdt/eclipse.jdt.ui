package invalid;

public class TestInterruptedExecutionFlow {
	public void main() {
		int i;
		i= /*]*/foo()/*[*/;
	}
	
	public int foo() {
		try {
			return 10;
		} catch(Exception e) {
			return 20;
		}
	}
}
