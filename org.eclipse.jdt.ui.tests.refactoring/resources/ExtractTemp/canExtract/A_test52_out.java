package p;

import java.math.BigDecimal;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

class A {
	private SortedMap fillSortedMap(Vector values, boolean byValue) {
		TreeMap map= new TreeMap();
		for (int i= 0; i < values.size(); i += 2) {
			Object valueOnIndexI= values.get(i);
			if (byValue) {
				map.put(
					values.get(i + 1),
					new Integer(((BigDecimal) valueOnIndexI).intValue()));
			} else {
				map.put(
					new Integer(((BigDecimal) valueOnIndexI).intValue()),
					values.get(i + 1));
			}
		}
		return map;
	}
}