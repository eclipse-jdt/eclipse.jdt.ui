package generics_out;

import java.util.ArrayList;
import java.util.List;

public class A_test1107 {
	public <E> void foo(E param) {
		extracted(param);
	}

	protected <E> void extracted(E param) {
		/*[*/List<E> list= new ArrayList<E>();
		foo(param);/*]*/
	}
}
