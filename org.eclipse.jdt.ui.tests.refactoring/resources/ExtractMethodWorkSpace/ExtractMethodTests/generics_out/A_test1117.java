package generics_out;

import java.util.Map;

public class A_test1117 {
	private <K, V> int test(Map<K, V> map) {
		return /*]*/extracted(map)/*[*/;
	}

	protected <K, V> int extracted(Map<K, V> map) {
		return map.size();
	}
}
