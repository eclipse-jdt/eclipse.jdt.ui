package expression_in;

public class TestAssignment {
	public void main() {
		int i= 0;
		i= /*]*/foo()/*[*/;
	}
	public int foo() {
		int x= 20;
		return x++;
	}
}
