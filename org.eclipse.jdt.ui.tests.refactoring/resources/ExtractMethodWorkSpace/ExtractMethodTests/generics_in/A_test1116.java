package generics_in;

import java.util.List;

public class A_test1116 {
	private <T, X> int test(List<T> list) {
		/*]*/X x= null;
		return list.size();/*[*/
	}
}
