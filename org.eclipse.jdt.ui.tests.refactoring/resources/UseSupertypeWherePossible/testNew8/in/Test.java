package p;

class Test{
	void test(){
		A a= new A();
		test(a);
	}
	void test(A a){
		a.foo();
	}
}