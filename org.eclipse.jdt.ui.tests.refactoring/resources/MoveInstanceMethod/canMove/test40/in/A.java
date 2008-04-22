package p;
class A {
	int id;
	boolean participates(B p) {
		return (p.participants[0].id == id);
    }
}