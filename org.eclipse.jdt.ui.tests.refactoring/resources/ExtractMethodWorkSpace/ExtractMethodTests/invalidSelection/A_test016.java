package invalidSelection;

public class A_test016 {
    public void foo() {
        Observer o = new <,>Observer() {
                public void update(Observable o, Object arg) {
                        /*]*/System.out.println(o);/*[*/
                }
        };              
}
}