package generic_out;

import java.util.HashMap;
import java.util.Map;

public class TestParameterizedType6 {
	void use() {
		Map<? extends Integer, ? super Number> vn1 = new HashMap<Integer, Number>();
		Map<? extends Number, ? super Integer> vn = /*]*/vn1/*[*/;
	}

	private Map<? extends Number, ? super Integer> me() {
		Map<? extends Integer, ? super Number> vn = new HashMap<Integer, Number>();
		return vn;
	}
}
