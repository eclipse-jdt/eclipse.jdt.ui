package controlStatement_in;

public class TestForTwo {
	public void main() {
		for (int i= 0; i < 10; i++)
			/*]*/foo();/*[*/
	}
	
	public void foo() {
		bar();
		bar();
	}
	public void bar() {
	}
}
