package p;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

class Qualified {
	String qualifier;
	String name;
}

class Comparisons {
	ArrayList fList= new ArrayList();
	
	void add(Qualified q) {
		fList.add(q);
	}
	
	void sort() {
		Collections.sort(fList, new Comparator() {
			public int compare(Object o1, Object o2) {
				Qualified q1= (Qualified) o1;
				Qualified q2= (Qualified) o2;
				int qualifierComp = q1.qualifier.compareTo(q2.qualifier);
				if (qualifierComp != 0)
					return qualifierComp;
				else
					return q1.name.compareTo(q2.name);
			}
		});
	}
}

class Comparisons2 {
	ArrayList fList= new ArrayList();
	
	void add(Qualified q) {
		fList.add(q);
	}
	
	void sort() {
		Collections.sort(fList, new Comparator() {
			public int compare(Object o1, Object o2) {
				Qualified q1= (Qualified) o1;
				Qualified q2= (Qualified) o2;
				int qualifierComp = q1.qualifier.compareTo(q2.qualifier);
				if (qualifierComp != 0)
					return qualifierComp;
				else
					return q1.name.compareTo(q2.name);
			}
		});
	}
}
