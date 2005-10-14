package p;

import java.util.ArrayList;

@SuppressWarnings("unused")
class A {

    private Number[] fNumbers;
    private Integer[] ints;

    public void process() {
        ArrayList<Integer> list= new ArrayList<Integer>();
        list.add(17);
        fNumbers= list.toArray(new Integer[list.size()]);
        ints= list.toArray(new Integer[list.size()]);
    }
}