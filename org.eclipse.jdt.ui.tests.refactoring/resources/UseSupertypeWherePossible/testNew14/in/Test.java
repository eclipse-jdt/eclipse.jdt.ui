package p;

class Test{
	void test(){
		A a= null;
		test(a);
	}
	void test(A b){
		b.foo();
	}
}