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
	
	AnnotationType getAnnotationType();
	
	boolean isTemporary();
	
	String getMessage();
	
	String[] getArguments();
	
	int getId();
	
	
	Image getImage(Display display);
	
	boolean isRelevant();
	
	boolean hasOverlay();
	
	Iterator getOverlaidIterator();
	
	void addOverlaid(IProblemAnnotation annotation);
	
	void removeOverlaid(IProblemAnnotation annotation);
	
	
	/**
	 * @deprecated	 */
	boolean isProblem();
	
	/**
	 * @deprecated
	 */
	boolean isTask();
	
	/**
	 * @deprecated
	 */
	boolean isWarning();
	
	/**
	 * @deprecated
	 */
	boolean isError();
}
