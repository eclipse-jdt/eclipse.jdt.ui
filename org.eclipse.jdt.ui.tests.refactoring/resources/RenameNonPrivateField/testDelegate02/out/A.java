package p;

class A {
   
	public String g;

	public String getG() {
		return g;
	}

	/**
	 * @deprecated Use {@link #getG()} instead
	 */
	public String getF() {
		return getG();
	}

	public void setG(String f) {
		this.g = f;
	}

	/**
	 * @deprecated Use {@link #setG(String)} instead
	 */
	public void setF(String f) {
		setG(f);
	}
}
