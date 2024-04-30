package bugs_in;

public class Test_issue_1360_1 {
    private boolean flag = false;
    public synchronized void originalMethod() throws InterruptedException {
        // Some logic here
        flag = true;
        notify();
    }
    public void callerMethod() throws InterruptedException {
        /*]*/originalMethod();/*[*/
    }

}