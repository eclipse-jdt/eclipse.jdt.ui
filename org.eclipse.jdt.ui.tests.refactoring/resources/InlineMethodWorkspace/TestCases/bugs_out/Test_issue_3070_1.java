package bugs_in;

public class Test_issue_3070_1 {
    public static void main(String[] args) {
        Test_issue_3070_1 obj = new Test_issue_3070_1();
        int result = obj.new H().g();
        System.out.println(result);
    }

    class H {
        int g() {
            return 7;
        }
    }
}
