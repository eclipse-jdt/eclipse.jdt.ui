package controlStatement_in;

public class TestIfElseTwo {
	public void main() {
		if (true)
			main();
		else
			/*]*/foo();/*[*/
	}
	
	public void foo() {
		bar();
		bar();
	}
	public void bar() {
	}
}
