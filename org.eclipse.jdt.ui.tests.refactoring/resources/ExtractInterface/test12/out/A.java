package p;
class A implements I{
	/* (non-Javadoc)
	 * @see p.I#m()
	 */
	public void m(){}
	private void f(){
		I a= new A();
	}
}