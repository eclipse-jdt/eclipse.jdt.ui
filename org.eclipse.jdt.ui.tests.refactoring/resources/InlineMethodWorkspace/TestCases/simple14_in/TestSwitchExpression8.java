package simple18_in;

import java.lang.String;

public class TestSwitchExpression1 {
	public static String format () {
		return "hello";
	}
	public static void main(String[] args) {
		int value = 0;
		String message = switch (value) {
			case 0 -> /*]*/format()/*[*/;
			default -> "";
		};
	}
}
