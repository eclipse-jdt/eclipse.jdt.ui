//9, 35, 9, 59
package p;

import java.util.List;
import java.util.ArrayList;

class A {
	private static final ArrayList<Integer> ITEMS= new ArrayList<Integer>();

	void m(){
		List<? extends Number> l= ITEMS;
	}	
}