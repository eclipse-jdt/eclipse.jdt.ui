package generic_in;

import java.util.Vector;

public class TestParameterizedType1 {
	void use () {
        Vector<? extends Number> vn = /*]*/me()/*[*/;
    }
    private Vector<? extends Number> me() {
        Vector<Integer> vn= new Vector<Integer>();
        vn.add(13);
        return vn;
    }
}
