package generic_in;

import java.util.HashMap;
import java.util.Map;

public class TestParameterizedType6 {
	void use() {
		Map<? extends Number, ? super Integer> vn = /*]*/me()/*[*/;
	}

	private Map<? extends Number, ? super Integer> me() {
		Map<? extends Integer, ? super Number> vn = new HashMap<Integer, Number>();
		return vn;
	}
}
