package generic_in;

import java.util.Vector;

public class TestParameterizedType4 {
	void use () {
		Vector<? extends Number> vn = /*]*/me()/*[*/;
    }
    private Vector<? extends Number> me() {
    	Vector<? extends Integer> vn= new Vector<Integer>();
        return vn;
    }
}
