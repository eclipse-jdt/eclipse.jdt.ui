package argument_out;

public class TestVarargs2 {

	public void bar(int i, String... args) {
		System.out.println(args[i]);
	}
	
	public void main() {
		String[] args = {"Hello", "Eclipse"};
		System.out.println(args[1]);
	}
}
