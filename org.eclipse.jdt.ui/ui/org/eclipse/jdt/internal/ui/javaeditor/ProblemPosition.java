package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;

import org.eclipse.jdt.core.compiler.IProblem;


/**
 * Position representing <code>IProblem</code>.
 */
public class ProblemPosition extends Position  implements IRegion {
	
	private IProblem fProblem;
	
	public ProblemPosition(IProblem problem) {
		super(problem.getSourceStart(), problem.getSourceEnd() + 1 - problem.getSourceStart());
		fProblem= problem;
	}
	
	public IProblem getProblem() {
		return fProblem;
	}
}
