package org.eclipse.jdt.internal.ui.packageview;

/*
 * COPIED unmodified from org.eclipse.ui.views.navigator
 */
 
/* package */ abstract class Frame {
	private int index = -1;
	private FrameList parent;
public Frame() {
	super();
}
public int getIndex() {
	return index;
}
public FrameList getParent() {
	return parent;
}
public abstract String getToolTipText();
public void setIndex(int index) {
	this.index = index;
}
public void setParent(FrameList parent) {
	this.parent = parent;
}
}
