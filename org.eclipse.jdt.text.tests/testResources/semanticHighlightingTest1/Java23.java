import module java.base;

public class Java23 {
	sealed record SampleRecord(String left, String right) {
		
	}
	non-sealed interface NSI {}
	void m() {
		var var = "Hello";
	}
}