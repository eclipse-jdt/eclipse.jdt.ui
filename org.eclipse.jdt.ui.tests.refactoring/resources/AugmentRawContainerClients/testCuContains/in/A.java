package p;
import java.util.ArrayList;
import java.util.List;

class A {
	void foo() {
		List l= new ArrayList();
		l.add("Eclipse");
		boolean has= l.contains("is");
	}
	void fooObj() {
		List lObj= new ArrayList();
		lObj.add("Eclipse");
		boolean has= lObj.contains(new Object());
	}
	void fooInteger() {
		List lInteger= new ArrayList();
		lInteger.add("Eclipse");
		boolean has= lInteger.contains(new Integer(1));
	}
}
