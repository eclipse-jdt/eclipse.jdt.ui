package p;

class A extends Object implements Cloneable, I{
	/* (non-Javadoc)
	 * @see p.I#m()
	 */
	public void m(){
	}
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
}