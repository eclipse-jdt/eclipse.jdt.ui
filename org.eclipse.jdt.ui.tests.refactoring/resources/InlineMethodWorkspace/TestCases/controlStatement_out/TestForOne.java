package controlStatement_out;

public class TestForOne {
	public void main() {
		for (int i= 0; i < 10; i++)
			bar();
	}
	
	public void foo() {
		bar();
	}
	public void bar() {
	}
}
