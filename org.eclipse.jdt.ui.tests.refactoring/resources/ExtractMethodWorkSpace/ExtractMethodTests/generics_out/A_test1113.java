package generics_out;

import java.util.List;

public class A_test1113<E> {
	private int test(List<E> list) {
		return /*]*/extracted(list)/*[*/;
	}

	protected int extracted(List<E> list) {
		return list.size();
	}
}
