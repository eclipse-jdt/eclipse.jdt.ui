package argument_out;

public class TestVarargs3 {

	public void bar(String... args) {
		for(String arg: args) {
			System.out.println(arg);
		}
	}
	
	public void main() {
		String[] args = {};
		for(String arg: args) {
			System.out.println(arg);
		}
	}
}
