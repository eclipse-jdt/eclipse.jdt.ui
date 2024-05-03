package invalidSelection;

public class A_testIssue1355 {

    public void foo(int a) {
        if (a < 3) {
            /*]*/return;/*[*/
        }
        System.out.println(a);
    }

}