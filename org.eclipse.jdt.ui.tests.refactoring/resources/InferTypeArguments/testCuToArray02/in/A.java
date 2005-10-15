package p;

import java.util.ArrayList;
import java.util.List;

public class A {
    
    String[] m() {
        List l= new ArrayList();
        l.add("Hello");
        return (String[]) l.toArray(new String[l.size()]);
    }
    String[][] m2() {
        List l= new ArrayList();
        l.add(new String[] {"Hello"});
        return (String[][]) l.toArray(new String[l.size()][]);
    }
    
    
    String[] test() {
        ArrayList list= new ArrayList();
        list.add(42);
        return (String[]) list.toArray(new String[list.size()]);
    }

    String[] test2() {
        ArrayList list= new ArrayList();
        return (String[]) list.toArray(new String[list.size()]);
    }
}
