package expression_in;

public class TestSimpleBody {
	public void main() {
		int i= 10 * /*]*/foo()/*[*/;
	}
	
	public int foo() {
		return 1 + 2;
	}
}
