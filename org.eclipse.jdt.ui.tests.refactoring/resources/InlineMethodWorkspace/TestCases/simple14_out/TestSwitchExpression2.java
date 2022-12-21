package simple18_in;

import java.lang.String;

public class TestSwitchExpression1 {
	public static String format (String... input) {
		return "";
	}
	public static void main(String[] args) {
		int value = 0;
		String message = switch (value) {
			case 0 : {
					String[] input = {""};
					yield "";
				}
			default : yield "";
		};
	}
}
