package p;

import java.util.ArrayList;
import java.util.List;

interface I {
	public List<String> getList();
}

class A implements I {
	public List<String> getList() {
		List<String> list= new ArrayList<String>();
		list.add("X");
		return list;
	}
}

class B extends A {
	public List<String> getList() {
		return null;
	}
}
