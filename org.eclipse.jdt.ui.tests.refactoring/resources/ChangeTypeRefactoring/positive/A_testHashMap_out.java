package p;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;


public class A_testHashMap_in {
	public static void main(String[] args) {
		A a = new B();
		Collection c1 = a.e();
		AbstractMap h1 = new HashMap();	/* A1 */
		a.h(h1, "aaa", c1);
		a.k(h1);
		for (Iterator it = h1.values().iterator(); it.hasNext(); ){
			Vector v1 = (Vector)it.next(); /* C1 */
			Integer i1 = (Integer)v1.iterator().next(); /* C2 */
			System.out.println(i1);
		}
	}
}

class A {
	public Vector e(){
		Vector v2 = new Vector();	/* A2 */
		Integer i2 = new Integer(17);
		insert(v2, i2);
		Integer i3 = (Integer)v2.iterator().next(); /* C3 */
		return v2;
	}
	public void f(){
		Vector v3 = new Vector();	/* A3 */
		v3.add(new Integer(18));
		reverse(v3);
		Integer i4 = (Integer)v3.get(0); /* C4 */
	}
	public void g(){
		Vector v4 = new Vector();	/* A4 */
		v4.add(new String("xyzzy"));
		reverse(v4);
		String s1 = (String)v4.get(0); /* C5 */
	}
	public void h(Map m1, Object o1, Collection c2){
		HashMap h2 = new HashMap(); /* A5 */
		h2.put(o1, c2);
		m1.putAll(h2);
	}
	public void k(Map m2){
		Vector v5 = new Vector(); /* A6 */
		v5.add(new Integer(18));
		v5.addAll(m2.keySet());
	}
	public void insert(Vector v6, Object o2){
		v6.add(o2);
	}
	public void reverse(List x1){
		for (int t=0; t < x1.size()/2; t++){
			Object temp = x1.get(x1.size()-t);
			x1.add(x1.size()-1, x1.get(t));
			x1.add(t, temp);
		}
	}
}
class B extends A {
	public void h(Map m3, Object o3, Collection c3){
		m3.put(o3, c3);
	}
}
