package p;
class Second {
	public void foo(Second s) {
		s.bar();
	}

	public void bar() {
	}
	
	public void go(int i, int j) {
	}

	public void print() {
		foo(this);
		bar();
		go(17, 18);
	}
}
