package p;
interface I1 {
	void /*target*/m(List<String> l);
}
interface I2 {
	void /*ripple*/m(List<String> l);
}
class I implements I1, I2 {
	void /*ripple*/m(List<String> l);
}

class A {
	void m(List<String> l);
}