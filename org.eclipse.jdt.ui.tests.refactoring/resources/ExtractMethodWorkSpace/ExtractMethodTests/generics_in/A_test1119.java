package generics_in;

import java.util.List;

public class A_test1119 {
	private <T> void test(List<T> list1, List<T> list2) {
		/*]*/list1= null;
		list2= null;/*[*/
	}
}
