package p;

class Test{
	void test(){
		B a= new A();
		test(a);
	}
	void test(B b){
		b.f= 0;
	}
}