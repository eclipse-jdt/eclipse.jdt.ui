package p;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

class A{
	List<String> fItems= new ArrayList<String>();
	
	public List<String> getItems() {
		return fItems;
	}
	
	public void setItems(List<String> list) {
		Items= list;
	}
}

class B {
	static {
		A a= new A();
		a.setItems(new LinkedList<String>());
		List<String> list= a.getItems();
		list.addAll(a.fItems);
	}
}