//selection: 11, 32, 11, 47
//name: hashMap -> hashMap
package simple;

import java.util.Map;
import java.util.HashMap;


public class NewInstance3 {
	Map<?, ?> test(HashMap<String, String> hashMap) {
		Map<String, String> m= hashMap;
		return m;
	}

	public static void test1(String[] args) {
		new NewInstance3().test(new HashMap<String, String>());
	}

	public static Map<?, ?> test2(NewInstance3 a) {
		Map<?, ?> bar= a.test(new HashMap<String, String>());
		return bar;
	}

	int test3(int a) {
		test(new HashMap<String, String>());
		return a;
	}
}
