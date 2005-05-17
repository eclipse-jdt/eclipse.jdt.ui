package generic_in;

import java.util.Vector;

public class TestParameterizedType2 {
	void use () {
        Vector<? super Integer> vn = /*]*/me()/*[*/;
    }
    private Vector<? super Integer> me() {
        Vector<? super Integer> vn= new Vector<Integer>();
        vn.add(13);
        return vn;
    }
}
