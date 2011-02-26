package generic_out;

import java.util.Vector;

public class TestParameterizedType5 {
	void use () {
        Vector<? super Number> vn1= new Vector<Number>();
		vn1.add(13);
		Vector<? super Integer> vn = /*]*/vn1/*[*/;
    }
    private Vector<? super Integer> me() {
        Vector<? super Number> vn= new Vector<Number>();
        vn.add(13);
        return vn;
    }
}
