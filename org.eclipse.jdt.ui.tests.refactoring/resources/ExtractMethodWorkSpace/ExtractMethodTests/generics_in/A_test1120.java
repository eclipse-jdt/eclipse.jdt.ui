package generics_in;

public class A_test1120 {

	<T extends Comparable<? super T>> void method(List<T> list) {
		/*[*/toExtract(list);/*]*/
	}

	static <T extends Comparable<? super T>> void toExtract(List<T> list) {
		return;
	}
}
