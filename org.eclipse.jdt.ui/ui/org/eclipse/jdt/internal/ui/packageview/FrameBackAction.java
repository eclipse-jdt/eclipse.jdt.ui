package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Generic back action, which calls <code>back()</code> on the frame list when run.
 * Enabled only when there is a frame after the current frame.
 */
/*
 * Copied unmodified from org.eclipse.ui.views.navigator
 */
 
/* package */ class FrameBackAction extends FrameAction {
public FrameBackAction(FrameList frameList) {
	super(frameList);
	setText("Back@ALT+ARROW_LEFT");
	setImageDescriptor(ImageDescriptor.createFromFile(FrameBackAction.class, "icons/basic/elcl16/bkward_nav.gif"));
	update();
}
/**
 * Calls <code>back()</code> on the frame list.
 */
public void run() {
	getFrameList().back();
}
protected void updateEnabledState() {
	setEnabled(getFrameList().getCurrentIndex() > 0);
}
protected void updateToolTip() {
	FrameList list = getFrameList();
	Frame frame = list.getFrame(list.getCurrentIndex()-1);
	if (frame != null) {
		String text = frame.getToolTipText();
		if (text != null && text.length() > 0) {
			setToolTipText("Back to " + text);
			return;
		}
	}
	setToolTipText("Back");
}
}
