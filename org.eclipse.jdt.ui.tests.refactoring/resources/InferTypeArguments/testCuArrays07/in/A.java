package p;

import java.util.ArrayList;

@SuppressWarnings("unused")
class A {

    private Number[] fNumbers;
    private Integer[] ints;

    public void process() {
        ArrayList list= new ArrayList();
        list.add(17);
        fNumbers= (Number[]) list.toArray(new Integer[list.size()]);
        ints= (Integer[]) list.toArray(new Integer[list.size()]);
    }
}