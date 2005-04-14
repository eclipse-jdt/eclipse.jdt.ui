package p;

import java.util.Vector;

class A {
    void m() {
        Vector vector = new Vector();
        vector.add("Hello");
        Vector v2= new Vector();
        v2.add((CharSequence) null);
        v2.addAll(vector);
    }
}