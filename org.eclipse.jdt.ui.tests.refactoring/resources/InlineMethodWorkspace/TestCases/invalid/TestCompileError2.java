package invalid;

public class TestCompileError2 {
	public void main() {
		/*]*/foo();/*[*/
	}
	
	public void foo() {
		main()
	}
}
