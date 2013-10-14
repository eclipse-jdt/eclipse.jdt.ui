package duplicates_out;
import java.util.List;

public class A_test989 {

    public  A_test989(int i) {
    }
    public  A_test989(List<String> l, int k) {
        this(extracted(l));
    }

    public void nsm(List<String> l) {
    	System.out.println(extracted(l));
    }
	protected static int extracted(List<String> l) {
		return /*[*/l.size()/*]*/;
	}

}