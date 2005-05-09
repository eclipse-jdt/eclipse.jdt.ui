package p;

import java.util.Vector;

public class A {
    public <T> T f1(T l) {
        Vector v = new Vector();
        v.add(l);
        return (T) v.get(0);
    }
}