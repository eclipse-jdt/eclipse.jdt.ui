package invalid;

public class TestInterruptedStatement {
	public void main() {
		/*]*/foo();/*[*/
	}
	
	public void foo() {
		if (true) {
			System.out.println("Eclipse");
			return;
		}	
	}
}
