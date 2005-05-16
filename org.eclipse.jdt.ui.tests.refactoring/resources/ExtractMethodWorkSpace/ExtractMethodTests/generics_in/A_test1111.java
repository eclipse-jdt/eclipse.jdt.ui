package generics_in;

import java.util.ArrayList;
import java.util.List;

public class A_test1111 {
	private List<? extends Number> al= new ArrayList<Integer>();
	
	void test () {
		/*]*/al.get(0)/*[*/;
	}	
}
