//selection: 12, 42, 12, 67
//name: hashMap -> hashMap
package simple;

import java.util.Map;

public class NewInstance4 {
	class HashMap {
	}

	void foo(java.util.HashMap<String, Number> hashMap) {
		Map<String, ? extends Number> m= hashMap;
	}

	public static void main(String[] args) {
		new NewInstance4().foo(new java.util.HashMap<String, Number>());
	}
}
