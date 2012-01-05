//selection: 10, 32, 10, 47
//name: hashMap -> hashMap
package simple;

import java.util.HashMap;
import java.util.Map;

class NewInstance2 {
	void foo() {
		Map<String, String> m= new HashMap<>();
	}

	public static void main(String[] args) {
		new NewInstance2().foo();
	}
}
