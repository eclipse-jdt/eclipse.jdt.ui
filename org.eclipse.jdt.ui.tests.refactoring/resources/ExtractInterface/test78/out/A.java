package p;
class A implements I {
    /* (non-Javadoc)
	 * @see p.I#m(p.I)
	 */
    public I m(I foo) {
        foo.m(foo);
        return null;
    }
}