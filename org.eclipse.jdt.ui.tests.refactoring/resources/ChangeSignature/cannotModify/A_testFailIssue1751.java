package p;

class A_testFailIssue1751 {
	// change method signature 'm' to 'k'
	public void m() {
	}

	class B {
		void k() {
			m();
		}
	}
}