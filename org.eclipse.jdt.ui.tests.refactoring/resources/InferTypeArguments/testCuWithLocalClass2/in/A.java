package p;

import java.util.ArrayList;

public class A {
    void m() {
        class Local<T> {
        	Local<T> fSelf;
        }
        new ArrayList().add(new Local<String>().fSelf);
    }
}
