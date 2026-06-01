package call_in;

interface Supplier<T> {
	T supply();
}

public class TestLambdaBody1 {

	// Refactoring operation: Inline method
	static String toInline() {
		return "lambda";
	}

	public static void main(String[] args) {
		Supplier<String> s= () -> /*]*/"lambda"/*[*/; // inline here
		System.out.println(s.get());
		System.out.println("done");
	}
}

