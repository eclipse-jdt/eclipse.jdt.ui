package p;
class Test<T> {
	void m(){
		Object object = new Test<String>();
		Test<String> test = (Test<String>) object;
		int i = test.n();
	}
	int n() {
		return 0;
	}
}
