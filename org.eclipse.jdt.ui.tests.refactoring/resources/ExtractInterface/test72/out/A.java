package p;
class A implements I {
    /* (non-Javadoc)
	 * @see p.I#m(p.I)
	 */
    public void m(I foo) {
        foo.m(foo);
    }
}