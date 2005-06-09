package generics_in;

import java.util.List;

public class A_test1118 {
	private <T> void test(List<T> list1, List<T> list2) {
		/*]*/list1.get(0);
		list2.get(0);/*[*/
	}
}
