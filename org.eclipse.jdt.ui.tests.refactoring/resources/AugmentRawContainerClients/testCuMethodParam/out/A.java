package p;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class A {
	void foo() {
		List<String> l= new ArrayList<String>();
		l.add("Eclipse");
		bar(l);
	}
	
	void bar(List<String> arg) {
		for (Iterator<String> iter= arg.iterator(); iter.hasNext();) {
			String element= iter.next();
			System.out.println(element);
		}
	}
}