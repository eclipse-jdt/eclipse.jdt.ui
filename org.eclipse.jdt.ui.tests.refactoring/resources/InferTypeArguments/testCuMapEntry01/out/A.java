package p;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

class A {
	void sets() {
		Map<String, Integer> map= new HashMap<String, Integer>();
		map.put("key", new Integer(17));
		Iterator<Entry<String, Integer>> iter= map.entrySet().iterator();
		Entry<String, Integer> entry= iter.next();
	}
}
