package p;
class Test<T> {
	void m(){
		Object object = new Test<String>();
		int i = ((Test<String>) object).n();
	}
	int n() {
		return 0;
	}
}
