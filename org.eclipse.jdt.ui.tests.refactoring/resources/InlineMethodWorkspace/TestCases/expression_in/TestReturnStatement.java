package expression_in;

public class TestReturnStatement {
	public int main() {
		return /*]*/foo()/*[*/;
	}
	
	public int foo() {
		if (true) {
			return 10;
		}
		return 20;
	}
}
