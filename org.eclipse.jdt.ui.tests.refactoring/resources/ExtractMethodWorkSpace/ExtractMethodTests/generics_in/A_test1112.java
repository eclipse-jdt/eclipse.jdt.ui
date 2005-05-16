package generics_in;

import java.util.ArrayList;
import java.util.List;

public class A_test1112 {
	private List<? super Integer> al= new ArrayList<Integer>();
	
	void test () {
		/*]*/al.get(0)/*[*/;
	}	
}
