package p;
import java.util.ArrayList;
import java.util.List;

class A {
	void foo() {
		List<String> l= new ArrayList<String>();
		l.add("Eclipse");
		boolean has= l.contains("is");
	}
	void fooObj() {
		List<String> lObj= new ArrayList<String>();
		lObj.add("Eclipse");
		boolean has= lObj.contains(new Object());
	}
	void fooInteger() {
		List<String> lInteger= new ArrayList<String>();
		lInteger.add("Eclipse");
		boolean has= lInteger.contains(new Integer(1));
	}
}
