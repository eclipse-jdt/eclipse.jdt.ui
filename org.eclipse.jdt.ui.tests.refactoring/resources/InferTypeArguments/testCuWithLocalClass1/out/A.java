package p;

import java.util.ArrayList;

public class A {
    void m() {
        class Local {
        	Local fSelf;
        }
        new ArrayList<Local>().add(new Local().fSelf);
    }
}
