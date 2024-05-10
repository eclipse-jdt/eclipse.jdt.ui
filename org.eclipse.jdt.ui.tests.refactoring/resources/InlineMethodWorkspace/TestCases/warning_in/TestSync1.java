package warning_in;

public class TestSync1 {
	public synchronized String format (String... input) {
		return "";
	}
	public String foo() {
		if (/*]*/format("abc")/*[*/ == "abc") {
			return "def";
		}
		return null;
	}
}