package expression_in;

public class TestSimpleExpressionWithStatements {
	public void main() {
		int i= /*]*/foo()/*[*/;
	}
	
	public int foo() {
		int x= 20;
		return x + 1;
	}
}
