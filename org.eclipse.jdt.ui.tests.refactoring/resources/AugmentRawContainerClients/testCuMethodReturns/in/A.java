package p;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class A {
	public void foo() {
		for (Iterator iter= getList().iterator(); iter.hasNext();) {
			String element= (String) iter.next();
			System.out.println(element);
		}
	}
	
	public List getList() {
		ArrayList result= new ArrayList();
		result.add("Tom");
		result.add("Jerry");
		return result;
	}
	
	public Iterator getIterator(List list) {
		list= getList();
		return list.iterator();
	}
	
	public Iterator getIterator2(List list2) {
		return null;
	}
}