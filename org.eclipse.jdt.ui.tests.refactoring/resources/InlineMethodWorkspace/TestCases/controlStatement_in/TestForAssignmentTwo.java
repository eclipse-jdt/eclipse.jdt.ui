package controlStatement_in;

public class TestForAssignmentTwo {
	public void main() {
		int x;
		for (int i= 0; i < 10; i++)
			x= /*]*/foo()/*[*/;
	}
	
	public int foo() {
		int x;
		return 20;
	}
}
