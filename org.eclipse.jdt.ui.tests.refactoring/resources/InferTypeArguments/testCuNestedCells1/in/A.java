package p;

class A {
	void m() {
		Cell c= new Cell();
		c.put("X");
		Cell nested= new Cell();
		nested.put(c);
	}
}
