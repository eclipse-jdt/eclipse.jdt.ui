package duplicates_out;

public class A_test988 {

    public static void sm() {
        extracted();
    }

    public void nsm() {
    	extracted();
    }

	protected static void extracted() {
		/*[*/shared();/*]*/
	}

    public static void shared() {
    }
}