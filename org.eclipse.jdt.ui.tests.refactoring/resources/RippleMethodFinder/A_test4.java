package p;
interface I1 {
	void /*target*/m(int i);
}
interface I2 {
	void /*ripple*/m(int integer);
}
interface I extends I1, I2 {
	
}