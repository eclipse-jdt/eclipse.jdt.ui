package p;

public class WildcardParam_in {
	public void foo() {
		NumberCell<Integer> c1= new NumberCell<Integer>(3);
		NumberCell<Integer> c1a= NumberCell.createNumberCell(c1);
		NumberCell<Float> c2= new NumberCell<Float>(3.14F);
		NumberCell<Float> c2a= NumberCell.createNumberCell(c2);
	}
}
class NumberCell<T extends Number> {
	public static <T extends Number> NumberCell<T> createNumberCell(NumberCell<? extends T> other) {
		return new NumberCell<T>(other);
	}
	T fNum;
	public NumberCell(T n) {
		fNum= n;
	}
	private /*[*/NumberCell/*]*/(NumberCell<? extends T> other) {
		fNum= other.fNum;
	}
}
