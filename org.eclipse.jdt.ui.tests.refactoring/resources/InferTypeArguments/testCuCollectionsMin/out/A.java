package p;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class A {
	public void foo() {
		String min= Collections.min(getNames());
	}
	public List<String> getNames() {
		ArrayList<String> result= new ArrayList<String>();
		result.add("Zwyssig");
		result.add("Abaecherli");
		return result;
	}
}