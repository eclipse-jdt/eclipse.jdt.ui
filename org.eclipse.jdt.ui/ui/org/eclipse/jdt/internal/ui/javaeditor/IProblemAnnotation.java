package org.eclipse.jdt.internal.ui.javaeditor;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.util.Iterator;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;


/**
 * Interface of annotations representing problems.
 */
public interface IProblemAnnotation {
	
	boolean isProblem();
	
	boolean isTask();
	
	String getMessage();
	
	String[] getArguments();
	
	int getId();
	
	
	boolean isTemporary();
	
	boolean isWarning();
	
	boolean isError();
	
	
	Image getImage(Display display);
	
	boolean isRelevant();
	
	boolean hasOverlay();
	
	
	Iterator getOverlaidIterator();
	
	void addOverlaid(IProblemAnnotation annotation);
	
	void removeOverlaid(IProblemAnnotation annotation);
}
