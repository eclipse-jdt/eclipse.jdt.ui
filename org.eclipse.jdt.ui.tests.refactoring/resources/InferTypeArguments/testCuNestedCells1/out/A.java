package p;

class A {
	void m() {
		Cell<String> c= new Cell<String>();
		c.put("X");
		Cell<Cell<String>> nested= new Cell<Cell<String>>();
		nested.put(c);
	}
}
