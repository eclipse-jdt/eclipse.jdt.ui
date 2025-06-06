package p;

import java.util.Vector;

public class A {
    public <T> T f1(T l) {
        Vector<T> v = new Vector<>();
        v.add(l);
        return v.get(0);
    }
}