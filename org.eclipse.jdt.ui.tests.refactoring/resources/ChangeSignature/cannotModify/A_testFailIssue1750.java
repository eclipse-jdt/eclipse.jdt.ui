package p;

class A_testFailIssue1750 {
	public void k(Number x) {
	}
	// change method signature 'm' to 'k'
	public void m(Long x) {
	}

	class B {
		void foo() {
			long i = 1;
			k(i);
		}
	}
}