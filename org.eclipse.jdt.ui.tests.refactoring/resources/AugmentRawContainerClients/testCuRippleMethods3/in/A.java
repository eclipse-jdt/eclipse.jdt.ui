package p;

import java.util.ArrayList;
import java.util.List;

class A {
	public List getList() {
		List list= new ArrayList();
		list.add("X");
		return list;
	}
}

interface I {
	public List getList();
}

class C extends A implements I {
	// Inherits getList() from A and I, but does NOT induce a connection
	// between them by redeclaring the method.
	//
	// Ways to deal:
	// A) Always calculate RippleMethods (but only once per method!).
	// B) If CU of C is found & processed:
	//     connect inherited methods from A with matching methods from I.
	//     If it is not guaranteed that C will be processed:
	//     need a subtype hierarchy on A and I  ==>  A) RippleMethods.
//	void use() {
//		List l= getList();
//	}
}
