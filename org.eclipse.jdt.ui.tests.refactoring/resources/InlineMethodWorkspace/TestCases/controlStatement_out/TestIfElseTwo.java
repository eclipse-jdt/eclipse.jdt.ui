package controlStatement_out;

public class TestIfElseTwo {
	public void main() {
		if (true)
			main();
		else {
			bar();
			bar();
		}
	}
	
	public void foo() {
		bar();
		bar();
	}
	public void bar() {
	}
}
