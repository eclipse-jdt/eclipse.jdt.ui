package controlStatement_out;

public class TestForTwo {
	public void main() {
		for (int i= 0; i < 10; i++) {
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
