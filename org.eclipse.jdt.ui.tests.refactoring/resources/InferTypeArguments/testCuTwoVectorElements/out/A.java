package p;

import java.util.ArrayList;
import java.util.Iterator;

class A {
	public static void exec() {
        ArrayList v1= new ArrayList();
        ArrayList<String> v2= new ArrayList<String>();
        v2.add("");
        Iterator iterator1 = v1.iterator();
        Iterator<String> iterator2 = v2.iterator();
	}
}
