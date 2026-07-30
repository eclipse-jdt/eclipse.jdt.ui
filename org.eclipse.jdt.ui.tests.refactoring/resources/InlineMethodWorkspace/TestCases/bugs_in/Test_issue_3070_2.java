package bugs_in;

public class Test_issue_3070_2 {
    public static void main(String[] args) {
        Test_issue_3070_2 obj = new Test_issue_3070_2();
        int result = obj.f();
        System.out.println(result);
    }

    int /*]*/f()/*[*/ {
    	class H {
    		int g() {
    			return 7;
    		}
    	}
        return new H().g();
    }
}
