/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.jface.text.Position;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.internal.ui.javaeditor.IProblemAnnotation;


public class ProblemPosition extends Position {

	private int fId;
	private String[] fArguments;
	private ICompilationUnit fCompilationUnit;
	
	public ProblemPosition(Position position, IProblemAnnotation annotation, ICompilationUnit cu) {
		this(position.getOffset(), position.getLength(), annotation.getId(), annotation.getArguments(), cu);
	}
	
	public ProblemPosition(IProblem problem, ICompilationUnit cu) {
		this(problem.getSourceStart(), problem.getSourceEnd() - problem.getSourceStart() + 1, problem.getID(), problem.getArguments(), cu);
	}	
	
	public ProblemPosition(int offset, int length, int id, String[] arguments, ICompilationUnit cu) {
		super(offset, length);
		fArguments= arguments;
		fId= id;
		fCompilationUnit= cu;		
	}
	
	public int getId() {
		return fId;
	}
	
	public String[] getArguments() {
		return fArguments;
	}
	/**
	 * Gets the compilationUnit.
	 * @return Returns a ICompilationUnit
	 */
	public ICompilationUnit getCompilationUnit() {
		return fCompilationUnit;
	}

}
