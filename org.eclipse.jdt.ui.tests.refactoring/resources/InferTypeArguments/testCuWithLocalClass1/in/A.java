package p;

import java.util.ArrayList;

public class A {
    void m() {
        class Local {
        	Local fSelf;
        }
        new ArrayList().add(new Local().fSelf);
    }
}
