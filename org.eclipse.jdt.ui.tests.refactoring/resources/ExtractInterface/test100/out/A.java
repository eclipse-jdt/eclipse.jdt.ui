/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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
