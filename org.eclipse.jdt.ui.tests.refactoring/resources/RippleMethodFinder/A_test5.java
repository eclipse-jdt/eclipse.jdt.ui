package p;
interface I1 {
	void /*target*/m(int i);
}
interface I2 {
	void /*ripple*/m(int integer);
}
class I implements I1, I2 {
	void /*ripple*/m(int in);
}