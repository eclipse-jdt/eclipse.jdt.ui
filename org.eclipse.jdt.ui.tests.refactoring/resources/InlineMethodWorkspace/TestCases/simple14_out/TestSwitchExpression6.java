package simple18_in;

public class TestSwitchExpression1 {
    public synchronized static String format (String... input) {
        return "";
    }
    public static String foo() {
        int value = 0;
        String message = switch (value) {
            case 0 -> /*]*/{
	String[] input = {""};
	synchronized (TestSwitchExpression1.class) {
		yield "";
	}
}
            default -> "";
        };
        return message;
    }
}
