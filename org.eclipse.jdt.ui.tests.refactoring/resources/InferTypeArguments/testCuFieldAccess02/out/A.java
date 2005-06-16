package p;

import java.util.*;

class Cell<T> {
	List<T> t;
	public void setT(AbstractList<T> t) {
		this.t= t;
	}
	public Collection<T> getT() {
		return t;
	}
}

class CellTest {
	public static void main(String[] args) {
		ArrayList<Boolean> booleanList= new ArrayList<Boolean>();
		booleanList.add(Boolean.FALSE);
		
		Cell<Boolean> c1= new Cell<Boolean>();
		c1.t= booleanList;
		c1.setT(booleanList);
		Iterable<Boolean> t= c1.t;
		Iterator<Boolean> iter= (Iterator<Boolean>) c1.t.iterator();
		Iterator<Boolean> iter2= c1.t.iterator();
		boolean bool= c1.t.iterator().next();
		
		Cell<Boolean> c2= new Cell<Boolean>();
		c2.t= booleanList;
	}
}
