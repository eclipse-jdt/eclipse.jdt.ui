package invalid;

public class TestCompileError3 {
	public void main() {
		/*]*/foo();/*[*/
	}
	
	public void foo() {
		{
	}
}
