package bugs_in;

public class Test_issue_2314_1 {
    @FunctionalInterface
    interface VoidFunction {
        void call();
    }
    static class Condition1 {
    	public static int /*]*/method/*[*/() { return 42; }
    }

    public static void main(String[] args) {
        VoidFunction v = Condition1::method;
    }
}
