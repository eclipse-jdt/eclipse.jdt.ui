package p;
class Test<T> {
	void m(){
		Object object = new Test<T>();
		Test<T> test = (Test<T>) object;
		int i = test.n();
	}
	int n() {
		return 0;
	}
}
