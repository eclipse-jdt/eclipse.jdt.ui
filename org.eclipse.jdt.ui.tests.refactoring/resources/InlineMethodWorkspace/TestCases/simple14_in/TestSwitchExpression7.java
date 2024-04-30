package simple18_in;

public class TestSwitchExpression1 {
	class Inner {
        public synchronized String format (String... input) {
            return "";
        }
	}
    public String foo() {
        int value = 0;
        Inner inner = new Inner();
        String message = switch (value) {
            case 0 -> /*]*/inner.format("")/*[*/;
            default -> "";
        };
        return message;
    }
}
