package bugs_out;

public class Test_44419_2 {

	public void foo() {
		String s= null;
		if (!/*]*/(s instanceof String)/*[*/) {
		}
	}
    
    boolean isString(Object o) {
        return o instanceof String;
    }
}
