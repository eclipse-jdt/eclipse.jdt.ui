package p;
class A{
	void m(boolean j){
		m(1);
	}

	/**
	 * @deprecated use instead m(boolean j)
	 */
	public void m(int i, int j) {
		m(j); // but what if the other direction - was value, now void?
	}
}