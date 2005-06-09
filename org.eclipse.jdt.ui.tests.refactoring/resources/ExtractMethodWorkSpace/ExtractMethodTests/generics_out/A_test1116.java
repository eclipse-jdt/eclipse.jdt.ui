package generics_out;

import java.util.List;

public class A_test1116 {
	private <T, X> int test(List<T> list) {
		/*]*/return extracted(list);/*[*/
	}

	protected <X, T> int extracted(List<T> list) {
		X x= null;
		return list.size();
	}
}
