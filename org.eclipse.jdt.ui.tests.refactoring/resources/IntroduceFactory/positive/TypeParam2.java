package p;

public class TypeParam2_in {
    public void foo() {
        Cell<String> cs1= Factory.createCell("");
        Cell<Integer> cs2= Factory.createCell(3);
        Cell<Float> cs3= Factory.createCell(3.14F);
    }
}
class Cell<T> {
    T fData;
    /*[*/Cell/*]*/(T t) {
        fData= t;
    }
}
class Factory {

	public static <T> Cell<T> createCell(T t) {
		return new Cell<T>(t);
	}
}
