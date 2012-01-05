//selection: 12, 42, 12, 67
//name: hashMap -> hashMap
package simple;

import java.util.Map;

public class NewInstance4 {
	class HashMap {
	}

	void foo() {
		Map<String, ? extends Number> m= new java.util.HashMap<>();
	}

	public static void main(String[] args) {
		new NewInstance4().foo();
	}
}
