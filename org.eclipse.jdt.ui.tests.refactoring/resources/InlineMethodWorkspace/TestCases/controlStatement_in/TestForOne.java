package controlStatement_in;

public class TestForOne {
	public void main() {
		for (int i= 0; i < 10; i++)
			/*]*/foo();/*[*/
	}
	
	public void foo() {
		bar();
	}
	public void bar() {
	}
}
