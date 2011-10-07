package generic_in;

import java.util.Vector;

public class TestParameterizedType5 {
	void use () {
        Vector<? super Integer> vn = /*]*/me()/*[*/;
    }
    private Vector<? super Integer> me() {
        Vector<? super Number> vn= new Vector<Number>();
        vn.add(13);
        return vn;
    }
}
