package argument_out;

public class TestVarargs {

	public void bar(String... args) {
		for(String arg: args) {
			System.out.println(arg);
		}
	}
	
	public void main() {
		String[] args = {"Hello", "Eclipse"};
		for(String arg: args) {
			System.out.println(arg);
		}
	}
}
