package p;

public class BoundedTypeParam_in {
	public void foo() {
		NumberCell<Integer> c1= new NumberCell<Integer>(3);
		NumberCell<Float> c2= new NumberCell<Float>(3.14F);
	}
}
class NumberCell<T extends Number> {
	T fData;
	public /*[*/NumberCell/*]*/(T t) {
		fData= t;
	}
}
