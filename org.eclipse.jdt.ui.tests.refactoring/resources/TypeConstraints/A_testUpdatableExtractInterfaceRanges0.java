package p;

interface Bag{
	public java.util.Iterator iterator();
	public A add(Comparable e);
	public A addAll(A v1);
}
class A implements Bag{
	int size = 0;
	Comparable[] elems = new Comparable[10];
	public java.util.Iterator iterator() {
		return new Iterator(this);
	}
	public A add(Comparable e) {
		if (size + 1 == elems.length) {
			Comparable[] newElems = new Comparable[2 * size];
			System.arraycopy(elems, 0, newElems, 0, size);
			elems = newElems;
		}
		elems[size++] = e;
		return this;
	}
	public A addAll(A v1) {
		java.util.Iterator i = v1.iterator();
		for (; i.hasNext(); add((Comparable) i.next()));
		return this;
	}
	public void sort() { /* insertion sort */
		for (int i = 1; i < size; i++) {
			Comparable e1 = elems[i];
			int j = i;
			while ((j > 0) && (elems[j - 1].compareTo(e1) > 0)) {
				elems[j] = elems[j - 1];
				j--;
			}
			elems[j] = e1;
		}
	}
}
class Iterator implements java.util.Iterator {
	private int count = 0;
	private A v2;
	Iterator(A v3) {
		v2 = v3;
	}
	public boolean hasNext() {
		return count < v2.size;
	}
	public Object next() {
		return v2.elems[count++];
	}
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
class Client {
	public static void main(String[] args) {
		A v4 = createList();
		populate(v4);
		update(v4);
		sortList(v4);
		print(v4);
	}
	static A createList() {
		return new A();
	}
	static void populate(A v5) {
		v5.add("foo").add("bar");
	}
	static void update(A v6) {
		A v7 = new A().add("zap").add("baz");
		v6.addAll(v7);
	}
	static void sortList(A v8) {
		v8.sort();
	}
	static void print(A v9) {
		for (java.util.Iterator iter = v9.iterator(); iter.hasNext();)
			System.out.println("Object: " + iter.next());
	}
}
