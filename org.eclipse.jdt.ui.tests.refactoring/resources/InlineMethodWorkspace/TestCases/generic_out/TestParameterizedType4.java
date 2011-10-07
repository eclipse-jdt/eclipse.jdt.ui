package generic_out;

import java.util.Vector;

public class TestParameterizedType4 {
	void use () {
		Vector<? extends Integer> vn1= new Vector<Integer>();
		Vector<? extends Number> vn = /*]*/vn1/*[*/;
    }
    private Vector<? extends Number> me() {
    	Vector<? extends Integer> vn= new Vector<Integer>();
        return vn;
    }
}
