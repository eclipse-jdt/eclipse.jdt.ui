package p;
interface I1 {
	void /*target*/m(List l);
}
interface I2 {
	void /*ripple*/m(List l);
}
class I implements I1, I2 {
	void /*ripple*/m(List l);
}