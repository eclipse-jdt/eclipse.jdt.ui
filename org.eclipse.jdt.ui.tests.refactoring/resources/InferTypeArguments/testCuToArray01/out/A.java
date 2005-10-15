package p;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("unused")
class A {
    {
        Collection<Integer> c= new LinkedList<Integer>();
        c.add(42);
        Generic g= new Generic();
        List<Integer> list= g.toList(c);
        ArrayList<Integer> arrayList= (ArrayList<Integer>) g.toList(c);
    }
}

class Generic<E> {
    public <T> List<T> toList(Collection<T> c) {
        return new ArrayList<T>(c);
    }
}