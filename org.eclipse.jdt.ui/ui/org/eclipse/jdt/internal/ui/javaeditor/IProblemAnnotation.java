package org.eclipse.jdt.internal.ui.javaeditor;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;


/**
 * Interface of annotations representing problems.
 */
public interface IProblemAnnotation {
	
	boolean isProblem();

	String getMessage();
	
	String[] getArguments();
	
	int getId();
	
	
	boolean isTemporaryProblem();
	
	boolean isWarning();
	
	boolean isError();
		
	boolean isRelevant();
	
	Image getImage(Display display);
}
