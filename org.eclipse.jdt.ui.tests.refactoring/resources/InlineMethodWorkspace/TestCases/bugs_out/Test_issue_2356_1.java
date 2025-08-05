package bugs_in;

public class Test_issue_2356_1 {
    @FunctionalInterface
    interface VoidFunction {
        int call();
    }
    static class Condition1 {
    }

    public static void main(String[] args) {
        VoidFunction v = () -> {return 42;};
    }
}
