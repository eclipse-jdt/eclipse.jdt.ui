package bugs_in;

public class Test_issue_2314_2 {
    @FunctionalInterface
    interface VoidFunction {
        void call();
    }
    static class Condition1 {
    }

    public static void main(String[] args) {
        VoidFunction v = () -> {String x = "abc"; @SuppressWarnings("unused") int x1 = x.length();};
    }
}
