package controlStatement_out;

public class TestIfThenTwo {
	public void main() {
		if (true) {
			bar();
			bar();
		} else
			main();
	}
	
	public void foo() {
		bar();
		bar();
	}
	public void bar() {
	}
}
