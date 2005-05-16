package generics_out;

import java.util.ArrayList;
import java.util.List;

public class A_test1112 {
	private List<? super Integer> al= new ArrayList<Integer>();
	
	void test () {
		/*]*/extracted()/*[*/;
	}

	protected Object extracted() {
		return al.get(0);
	}	
}
