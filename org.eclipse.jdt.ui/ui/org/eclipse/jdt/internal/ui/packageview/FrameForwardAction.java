/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Generic forward action, which calls <code>forward()</code> on the frame list when run.
 * Enabled only when there is a frame after the current frame.
 */
/* 
 * COPIED unmodified from org.eclipse.ui.views.navigator
 */
/* package */ class FrameForwardAction extends FrameAction {
public FrameForwardAction(FrameList frameList) {
	super(frameList);
	setText("Forward@ALT+ARROW_RIGHT");
	setImageDescriptor(ImageDescriptor.createFromFile(FrameForwardAction.class, "icons/basic/elcl16/forward_nav.gif"));
	update();
}
/**
 * Calls <code>forward()</code> on the frame list.
 */
public void run() {
	getFrameList().forward();
}
protected void updateEnabledState() {
	FrameList list = getFrameList();
	setEnabled(list.getCurrentIndex() < list.size()-1);
}
protected void updateToolTip() {
	FrameList list = getFrameList();
	Frame frame = list.getFrame(list.getCurrentIndex()+1);
	if (frame != null) {
		String text = frame.getToolTipText();
		if (text != null && text.length() > 0) {
			setToolTipText("Forward to " + text);
			return;
		}
	}
	setToolTipText("Forward");
}
}
