package org.eclipse.jdt.internal.ui.text.java;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jdt.core.compiler.IProblem;



/**
 * Interface of an object that can deal with a set of problems.
 */
public interface IProblemAcceptor {
	
	/**
	 * Accepts a list of problems.
	 */
	void setProblems(IProblem[] problems);
}
