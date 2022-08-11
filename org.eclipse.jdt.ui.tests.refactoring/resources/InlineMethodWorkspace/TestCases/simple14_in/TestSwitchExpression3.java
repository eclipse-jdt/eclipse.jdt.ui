package simple18_in;

import java.lang.String;

public class TestSwitchExpression1 {
	public static String format (String... input) {
		if (input == null) {
			return "null";
		} else if (input.length > 1) {
			return "multiple";
		} else {
			return "single";
		}
	}
	public static void main(String[] args) {
		int value = 0;
		String message = switch (value) {
			case 0 -> /*]*/format("")/*[*/;
			default -> "";
		};
	}
}
