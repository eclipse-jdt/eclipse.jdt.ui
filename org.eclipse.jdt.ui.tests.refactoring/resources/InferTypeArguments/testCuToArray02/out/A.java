package p;

import java.util.ArrayList;
import java.util.List;

public class A {
    
    String[] m() {
        List<String> l= new ArrayList<>();
        l.add("Hello");
        return l.toArray(new String[l.size()]);
    }
    String[][] m2() {
        List<String[]> l= new ArrayList<>();
        l.add(new String[] {"Hello"});
        return l.toArray(new String[l.size()][]);
    }
    
    
    String[] test() {
        ArrayList<Integer> list= new ArrayList<>();
        list.add(42);
        return list.toArray(new String[list.size()]);
    }

    String[] test2() {
        ArrayList list= new ArrayList();
        return (String[]) list.toArray(new String[list.size()]);
    }
}
