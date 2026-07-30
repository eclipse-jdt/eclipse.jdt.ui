package bugs_in;

public class Test_issue_3070_2 {
    public static void main(String[] args) {
        Test_issue_3070_2 obj = new Test_issue_3070_2();
		class H {
			int g() {
				return 7;
			}
		}
        int result = new H().g();
        System.out.println(result);
    }
}
