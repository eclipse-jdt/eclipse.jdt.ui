package p;

class A {
	int size = 0;
	Comparable[] elems = new Comparable[10];
	public java.util.Iterator iterator() {
		return new Iterator(this);
	}
	public A add(Comparable e) {
		if (this.size + 1 == this.elems.length) {
			Comparable[] newElems = new Comparable[2 * this.size];
			System.arraycopy(this.elems, 0, newElems, 0, this.size);
			this.elems = newElems;
		}
		this.elems[this.size++] = e;
		return this;
	}
	public A addAll(A v1) {
		java.util.Iterator i = v1.iterator();
		for (; i.hasNext(); this.add((Comparable) i.next()));
		return this;
	}
	public void sort() { /* insertion sort */
		for (int i = 1; i < this.size; i++) {
			Comparable e1 = this.elems[i];
			int j = i;
			while ((j > 0) && (this.elems[j - 1].compareTo(e1) > 0)) {
				this.elems[j] = this.elems[j - 1];
				j--;
			}
			this.elems[j] = e1;
		}
	}
}
class Iterator implements java.util.Iterator {
	private int count = 0;
	private A v2;
	Iterator(A v3) {
		this.v2 = v3;
	}
	public boolean hasNext() {
		return this.count < this.v2.size;
	}
	public Object next() {
		return this.v2.elems[this.count++];
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
