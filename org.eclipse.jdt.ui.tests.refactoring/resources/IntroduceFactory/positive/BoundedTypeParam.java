package p;

public class BoundedTypeParam_in {
	public void foo() {
		NumberCell<Integer> c1= NumberCell.createNumberCell(3);
		NumberCell<Float> c2= NumberCell.createNumberCell(3.14F);
	}
}
class NumberCell<T extends Number> {
	T fData;
	public static <T extends Number> NumberCell<T> createNumberCell(T t) {
		return new NumberCell<T>(t);
	}
	private /*[*/NumberCell/*]*/(T t) {
		fData= t;
	}
}
