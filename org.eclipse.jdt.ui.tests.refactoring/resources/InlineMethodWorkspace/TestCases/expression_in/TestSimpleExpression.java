package expression_in;

public class TestSimpleExpression {
	public void main() {
		int i= /*]*/foo()/*[*/;
	}
	
	public int foo() {
		return 1 + 1;
	}
}
