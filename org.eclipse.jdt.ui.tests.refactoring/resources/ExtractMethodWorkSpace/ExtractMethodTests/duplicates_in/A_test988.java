package duplicates_in;

public class A_test988 {

    public static void sm() {
        shared();
    }

    public void nsm() {
    	/*[*/shared();/*]*/
    }

    public static void shared() {
    }
}