package generics_out;

import java.util.List;

public class A_test1114 {
	private <T> int test(List<T> list) {
		return /*]*/extracted(list)/*[*/;
	}

	protected <T> int extracted(List<T> list) {
		return list.size();
	}
}
