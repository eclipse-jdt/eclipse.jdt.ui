package generics_in;

import java.util.Map;

public class A_test1117 {
	private <K, V> int test(Map<K, V> map) {
		return /*]*/map.size()/*[*/;
	}
}
