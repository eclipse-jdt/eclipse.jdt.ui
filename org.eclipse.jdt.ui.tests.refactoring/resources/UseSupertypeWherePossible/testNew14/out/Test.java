package p;

class Test{
	void test(){
		B a= null;
		test(a);
	}
	void test(B b){
		b.foo();
	}
}