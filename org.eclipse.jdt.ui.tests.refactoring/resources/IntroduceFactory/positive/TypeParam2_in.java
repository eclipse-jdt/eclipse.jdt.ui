package p;

public class TypeParam2_in {
    public void foo() {
        Cell<String> cs1= new Cell<String>("");
        Cell<Integer> cs2= new Cell<Integer>(3);
        Cell<Float> cs3= new Cell<Float>(3.14F);
    }
}
class Cell<T> {
    T fData;
    public /*[*/Cell/*]*/(T t) {
        fData= t;
    }
}
class Factory {
}
