package p;

class Test{
	void test(){
		A a= new A();
		test(a);
	}
	void test(B b){
		b.f= 0;
	}
}