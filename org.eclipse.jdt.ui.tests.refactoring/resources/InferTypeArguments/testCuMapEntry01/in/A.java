package p;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class A {
	void sets() {
		Map map= new HashMap();
		map.put("key", Integer.valueOf(17));
		Iterator iter= map.entrySet().iterator();
		Map.Entry entry= (Map.Entry) iter.next();
	}
}
