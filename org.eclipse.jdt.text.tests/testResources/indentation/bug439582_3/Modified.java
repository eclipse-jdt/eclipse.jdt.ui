package indentbug;

public class Bug3 {
	public static void main(String[] args) {
		String test = "abc"
				+ String.format("%s %s", "abc abc abc abc abc abc abc",
						"abc abc abc abc abc abc abc abc abc abc abc abc ")
				+ String.<String> format("%s %s", "abc abc abc abc abc abc abc",
						"abc abc abc abc abc abc abc abc abc abc abc abc ")
				+ (String) String.format("%s %s", "abc abc abc abc abc abc abc",
						"abc abc abc abc abc abc abc abc abc abc abc abc ")
				+ String.format("%s %s", "abc abc abc abc abc abc abc",
						"abc abc abc abc abc abc abc abc abc abc abc abc ");
	}
}
