package argument_out;

public class TestVarargs4 {

	public void bar(String... args) {
		for(String arg: args) {
			System.out.println(arg);
		}
	}
	
	public void main() {
		String[] strings= {"Hello", "Eclipse"};
		for(String arg: strings) {
			System.out.println(arg);
		}
	}
}
