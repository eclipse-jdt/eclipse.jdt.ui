package p;
class A{
	enum TEST {
		PROBE;
	}
	void m(int i){
		A.TEST temp= TEST.PROBE;
		TEST x= temp;
	}
}