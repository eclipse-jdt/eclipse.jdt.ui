package p;

import java.util.ArrayList;
import java.util.List;

interface I {
	public List getList();
}

class A implements I {
	public List getList() {
		List list= new ArrayList();
		list.add("X");
		return list;
	}
}

class B extends A {
	public List getList() {
		return null;
	}
}
