package p;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class A {
	void foo() {
		List<String> l= new ArrayList<String>();
		l.add("Eclipse"); l.add("is"); l.add(new String("cool"));
		for (Iterator<String> iter= l.iterator(); iter.hasNext();) {
			String word= iter.next();
			System.out.print(word);
			System.out.print(" ");
		}
	}
}