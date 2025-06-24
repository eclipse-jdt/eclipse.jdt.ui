package p;

class A {
	void m() {
		Cell<String> c= new Cell<>();
		c.put("X");
		Cell<Cell<String>> nested= new Cell<>();
		nested.put(c);
	}
}
