package argument_in;

public class TestVarargs5 {

	public void bar(Object... args) {
		for(Object arg: args) {
			System.out.println(arg);
		}
	}
	
	public void main() {
		String[] strings= {"Hello", "Eclipse"};
		/*]*/bar(strings);/*[*/
	}
}
