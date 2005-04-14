package p;

import java.util.Vector;

class A {
    void m() {
        Vector<String> vector = new Vector<String>();
        vector.add("Hello");
        Vector<CharSequence> v2= new Vector<CharSequence>();
        v2.add((CharSequence) null);
        v2.addAll(vector);
    }
}