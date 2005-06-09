package generics_in;

import java.util.List;

public class A_test1114 {
	private <T> int test(List<T> list) {
		return /*]*/list.size()/*[*/;
	}
}
