//9, 35, 9, 59
package p;

import java.util.List;
import java.util.ArrayList;

class A {
	void m(){
		List<? extends Number> l= new ArrayList<Integer>();
	}	
}