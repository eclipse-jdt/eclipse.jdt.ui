package p;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class A {
	public void foo() {
		String min= (String) Collections.min(getNames());
	}
	public List getNames() {
		ArrayList result= new ArrayList();
		result.add("Zwyssig");
		result.add("Abaecherli");
		return result;
	}
}