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
		ArrayList booleanList= new ArrayList();
		booleanList.add(Boolean.FALSE);
		
		Cell c1= new Cell();
		c1.t= booleanList;
		c1.setT(booleanList);
		Iterable t= c1.t;
		Iterator iter= (Iterator) c1.t.iterator();
		Iterator iter2= c1.t.iterator();
		boolean bool= (Boolean) c1.t.iterator().next();
		
		Cell c2= new Cell();
		c2.t= booleanList;
	}
}
