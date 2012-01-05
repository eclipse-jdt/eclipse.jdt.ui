//selection: 11, 32, 11, 47
//name: hashMap -> hashMap
package simple;

import java.util.Map;
import java.util.HashMap;


public class NewInstance3 {
	Map<?, ?> test() {
		Map<String, String> m= new HashMap<>();
		return m;
	}

	public static void test1(String[] args) {
		new NewInstance3().test();
	}

	public static Map<?, ?> test2(NewInstance3 a) {
		Map<?, ?> bar= a.test();
		return bar;
	}

	int test3(int a) {
		test();
		return a;
	}
}
