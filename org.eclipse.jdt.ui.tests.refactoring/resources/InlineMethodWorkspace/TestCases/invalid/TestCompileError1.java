package invalid;

public class TestCompileError1 {
	public void main() {
		/*]*/foo();/*[*/
	}
	
	public void foo() {
		T t= null;
	}
}
