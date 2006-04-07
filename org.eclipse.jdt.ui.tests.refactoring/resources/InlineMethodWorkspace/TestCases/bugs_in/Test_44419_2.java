package bugs_in;

public class Test_44419_2 {

	public void foo() {
		String s= null;
		if (!/*]*/isString(s)/*[*/) {
		}
	}
    
    boolean isString(Object o) {
        return o instanceof String;
    }
}
