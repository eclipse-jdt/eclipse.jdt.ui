package simple18_out;

import java.lang.String;

public class TestSwitchExpression1 {
	public static String format () {
		return "hello";
	}
	public static void main(String[] args) {
		int value = 0;
		String message = switch (value) {
			case 0 -> /*]*/"hello"/*[*/;
			default -> "";
		};
	}
}
