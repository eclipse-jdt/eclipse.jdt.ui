package p;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class A {
	void foo() {
		List l= new ArrayList();
		l.add("Eclipse"); l.add("is"); l.add(new String("cool"));
		for (Iterator iter= l.iterator(); iter.hasNext();) {
			String word= (String) iter.next();
			System.out.print(word);
			System.out.print(" ");
		}
	}
}