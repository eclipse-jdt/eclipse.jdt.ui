package bugs_in;

public class Test_issue_1360_2 {
    public static boolean flag = false;
    public synchronized static void originalMethod() {
        // Some logic here
        flag = true;
        System.out.println("here");
    }
    public static void callerMethod() {
        /*]*/synchronized (Test_issue_1360_2.class) {
			// Some logic here
			flag = true;
			System.out.println("here");
		}/*[*/
    }

}