package argument_out;

public class TestVarargs5 {

	public void bar(Object... args) {
		for(Object arg: args) {
			System.out.println(arg);
		}
	}
	
	public void main() {
		String[] strings= {"Hello", "Eclipse"};
		for(Object arg: strings) {
			System.out.println(arg);
		}
	}
}
