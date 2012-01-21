package generics_out;

public class A_test1120 {

	<T extends Comparable<? super T>> void method(List<T> list) {
		extracted(list);
	}

	protected <T extends Comparable<? super T>> void extracted(List<T> list) {
		/*[*/toExtract(list);/*]*/
	}

	static <T extends Comparable<? super T>> void toExtract(List<T> list) {
		return;
	}
}
