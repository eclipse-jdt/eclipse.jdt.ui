package generics_out;

import java.util.ArrayList;
import java.util.List;

public class A_test1111 {
	private List<? extends Number> al= new ArrayList<Integer>();
	
	void test () {
		/*]*/extracted()/*[*/;
	}

	protected Number extracted() {
		return al.get(0);
	}	
}
