import module java.base;

public class Java24 {
	sealed record SampleRecord(String left, String right) {
		
	}
	non-sealed interface NSI {}
	void m() {
		var var = "Hello";
	}
}