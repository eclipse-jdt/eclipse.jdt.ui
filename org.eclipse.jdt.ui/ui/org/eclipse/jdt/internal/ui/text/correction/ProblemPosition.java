package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.jface.text.Position;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.ui.javaeditor.IProblemAnnotation;


public class ProblemPosition extends Position {

	private IProblemAnnotation fAnnotation;
	private ICompilationUnit fCompilationUnit;
	
	public ProblemPosition(Position position, IProblemAnnotation annotation, ICompilationUnit cu) {
		super(position.getOffset(), position.getLength());
		fAnnotation= annotation;
		fCompilationUnit= cu;
	}
		
	public String getMessage() {
		return fAnnotation.getMessage();
	}
	
	public int getId() {
		return fAnnotation.getId();
	}
	
	public String[] getArguments() {
		return fAnnotation.getArguments();
	}
	/**
	 * Gets the compilationUnit.
	 * @return Returns a ICompilationUnit
	 */
	public ICompilationUnit getCompilationUnit() {
		return fCompilationUnit;
	}

}
