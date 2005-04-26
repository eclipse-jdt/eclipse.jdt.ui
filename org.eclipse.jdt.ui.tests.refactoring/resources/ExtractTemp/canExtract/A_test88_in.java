package p;

import java.util.List;

public class A {
    public List<?> foo() {
        return null;
    }
    
    void take(List<?> a) {
        
    }
    void bar() {
        take(foo());
    }
}
