package p;
class Inner{
	void foo() {
		A.F= 1;
		A.F= 2;
		p.A.F= 3;
	}
}