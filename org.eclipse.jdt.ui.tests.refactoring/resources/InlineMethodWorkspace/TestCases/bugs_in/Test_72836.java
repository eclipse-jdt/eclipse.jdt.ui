package bugs_in;

public class Test_72836 {
    private void foo() {
        System.out.println();
    }
    
    private void bar() {
        this./*]*/foo()/*[*/;
    }
}