package p;
class Test<T> {
	void m(){
		Object object = new Test<T>();
		int i = ((Test<T>) object).n();
	}
	int n() {
		return 0;
	}
}
