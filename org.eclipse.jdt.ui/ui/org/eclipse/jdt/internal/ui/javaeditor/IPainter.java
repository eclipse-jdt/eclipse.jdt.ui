package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


public interface IPainter {
	
	void dispose();
	
	void paint();
	
	void deactivate(boolean redraw);
	
	void setPositionManager(IPositionManager manager);
}
