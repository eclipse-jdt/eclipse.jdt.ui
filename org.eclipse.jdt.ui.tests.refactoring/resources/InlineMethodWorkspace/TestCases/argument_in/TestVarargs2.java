package argument_in;

public class TestVarargs2 {

	public void bar(int i, String... args) {
		System.out.println(args[i]);
	}
	
	public void main() {
		/*]*/bar(1, "Hello", "Eclipse");/*[*/
	}
}
