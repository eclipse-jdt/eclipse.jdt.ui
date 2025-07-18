package bugs_in;

public class Test_issue_2356_2 {
    @FunctionalInterface
    interface VoidFunction {
        int call();
    }
    static class Condition1 {
    	public static int /*]*/method/*[*/() { String x = "abc"; return x.length(); }
    }

    public static void main(String[] args) {
        VoidFunction v = Condition1::method;
    }
}
