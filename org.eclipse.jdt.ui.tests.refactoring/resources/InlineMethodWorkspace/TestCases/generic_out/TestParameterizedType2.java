package generic_out;

import java.util.Vector;

public class TestParameterizedType2 {
	void use () {
        Vector<? super Integer> vn1= new Vector<Integer>();
		vn1.add(13);
		Vector<? super Integer> vn = /*]*/vn1/*[*/;
    }
    private Vector<? super Integer> me() {
        Vector<? super Integer> vn= new Vector<Integer>();
        vn.add(13);
        return vn;
    }
}
