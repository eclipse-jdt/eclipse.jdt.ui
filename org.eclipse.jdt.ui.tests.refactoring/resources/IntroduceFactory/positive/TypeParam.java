package p;

public class TypeParam_in {
    public void foo() {
        Cell<String> cs1= Cell.createCell("");
        Cell<Integer> cs2= Cell.createCell(3);
        Cell<Float> cs3= Cell.createCell(3.14F);
    }
}
class Cell<T> {
	public static <T> Cell<T> createCell(T t) {
		return new Cell<T>(t);
	}
	T fData;
	private /*[*/Cell/*]*/(T t) {
		fData= t;
	}
}
