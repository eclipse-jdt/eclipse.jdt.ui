package controlStatement_in;

public class TestIfThenTwo {
	public void main() {
		if (true)
			/*]*/foo();/*[*/
		else
			main();
	}
	
	public void foo() {
		bar();
		bar();
	}
	public void bar() {
	}
}
