package p;

import java.util.ArrayList;
import java.util.List;

class A {
	void addAll(String[] ss) {
		List l= A.asList(ss);
	}
	
	public static <T> List<T> asList(T[] a) {
		ArrayList<T> res= new ArrayList<T>();
		for (int i= 0; i < a.length; i++) {
			res.add(a[i]);
		}
		return res;
	}
}