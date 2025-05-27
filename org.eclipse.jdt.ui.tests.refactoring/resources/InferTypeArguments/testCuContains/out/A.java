package p;
import java.util.ArrayList;
import java.util.List;

class A {
	void foo() {
		List<String> l= new ArrayList<>();
		l.add("Eclipse");
		boolean has= l.contains("is");
	}
	void fooObj() {
		List<String> lObj= new ArrayList<>();
		lObj.add("Eclipse");
		boolean has= lObj.contains(new Object());
	}
	void fooInteger() {
		List<String> lInteger= new ArrayList<>();
		lInteger.add("Eclipse");
		boolean has= lInteger.contains(Integer.valueOf(1));
	}
}
