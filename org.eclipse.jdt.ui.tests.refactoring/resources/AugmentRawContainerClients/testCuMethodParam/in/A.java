package p;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class A {
	void foo() {
		List l= new ArrayList();
		l.add("Eclipse");
		bar(l);
	}
	
	void bar(List arg) {
		for (Iterator iter= arg.iterator(); iter.hasNext();) {
			String element= (String) iter.next();
			System.out.println(element);
		}
	}
}