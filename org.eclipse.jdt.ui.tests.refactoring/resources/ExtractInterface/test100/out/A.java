package p;

public abstract class A implements I {
	/* (non-Javadoc)
	 * @see p.I#x()
	 */
	public abstract void x()   ;
	//	 TestRunListener implementation
	/* (non-Javadoc)
	 * @see p.I#y()
	 */
	public abstract void y() /* nasty */;
	/* (non-Javadoc)
	 * @see p.I#z()
	 */
	public abstract void z() // xx
	;

	/* (non-Javadoc)
	 * @see p.I#a()
	 */abstract public void a () /**post-Javadoc*/;

	/* (non-Javadoc)
	 * @see p.I#b()
	 */
	
	//abstract
	
	public abstract void b();
	
	//destruct
	/* (non-Javadoc)
	 * @see p.I#c()
	 */
	public
	abstract
	void c
	()
	/* Comments*/
	/** en */
	// gros!
	; //post
	/* (non-Javadoc)
	 * @see p.I#d()
	 */
	public abstract void d();
}
