package p;

public class WildcardParam_in {
	public void foo() {
		NumberCell<Integer> c1= new NumberCell<Integer>(3);
		NumberCell<Integer> c1a= new NumberCell<Integer>(c1);
		NumberCell<Float> c2= new NumberCell<Float>(3.14F);
		NumberCell<Float> c2a= new NumberCell<Float>(c2);
	}
}
class NumberCell<T extends Number> {
	T fNum;
	public NumberCell(T n) {
		fNum= n;
	}
	public /*[*/NumberCell/*]*/(NumberCell<? extends T> other) {
		fNum= other.fNum;
	}
}
