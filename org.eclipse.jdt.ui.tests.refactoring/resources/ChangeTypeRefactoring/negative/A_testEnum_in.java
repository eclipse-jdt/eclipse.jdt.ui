enum E {
	A(1), B(2), C(3);
	public E(int i) {
	}
}

class Z {
	E foo() {
		E e= null;   //<=== disable generalize type here.
		return e;
	}
}
