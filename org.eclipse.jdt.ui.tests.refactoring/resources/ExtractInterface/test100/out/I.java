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


/** typecomment template*/
public interface I {

	public abstract void x()   ;

	//	 TestRunListener implementation
	public abstract void y() /* nasty */;

	public abstract void z() // xx
	;

	/** Javadoc*/abstract public void a () /**post-Javadoc*/;

	/**JD*/
	
	//abstract
	
	public abstract void b();

	//destruct
	public
	abstract
	void c
	()
	/* Comments*/
	/** en */
	// gros!
	; //post

	public abstract void d();

}