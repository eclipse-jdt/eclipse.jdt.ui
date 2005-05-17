package generic_out;

import java.util.Vector;

public class TestParameterizedType1 {
	void use () {
        Vector<Integer> vn1= new Vector<Integer>();
		vn1.add(13);
		Vector<? extends Number> vn = /*]*/vn1/*[*/;
    }
    private Vector<? extends Number> me() {
        Vector<Integer> vn= new Vector<Integer>();
        vn.add(13);
        return vn;
    }
}
