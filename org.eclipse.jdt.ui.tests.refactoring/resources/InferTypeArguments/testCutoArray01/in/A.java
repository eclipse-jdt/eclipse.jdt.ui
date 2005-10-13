package p;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("unused")
class A {
    {
        Collection c= new LinkedList();
        c.add(42);
        Generic g= new Generic();
        List list= g.toList(c);
        ArrayList arrayList= (ArrayList) g.toList(c);
    }
}

class Generic<E> {
    public <T> List<T> toList(Collection<T> c) {
        return new ArrayList<T>(c);
    }
}