package argument_in;

public class TestVarargs3 {

	public void bar(String... args) {
		for(String arg: args) {
			System.out.println(arg);
		}
	}
	
	public void main() {
		/*]*/bar();/*[*/
	}
}
