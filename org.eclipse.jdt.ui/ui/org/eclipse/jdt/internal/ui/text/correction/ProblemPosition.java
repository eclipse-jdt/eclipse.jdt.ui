package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.jface.text.Position;

import org.eclipse.jdt.internal.ui.javaeditor.IProblemAnnotation;


public class ProblemPosition extends Position {

	private IProblemAnnotation fAnnotation;
	
	public ProblemPosition(Position position, IProblemAnnotation annotation) {
		super(position.getOffset(), position.getLength());
		fAnnotation= annotation;
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
}
