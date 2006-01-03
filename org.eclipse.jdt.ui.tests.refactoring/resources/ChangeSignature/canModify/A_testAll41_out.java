package p;
class A{
	/**
	 * @deprecated Use {@link #m()} instead
	 */
	void m(int i){
		m();
	}

	void m(){
		m();
	}
}