package p;

class A {
	void newName(String y){
        newName(y);
    }
}

class B extends A {
	public void newName(String q) {
		new A().newName("x");
		return newName("k");
	}
}