package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

/**
 * Abstract superclass for actions dealing with frames or a frame list.
 * This listens for changes to the frame list and updates itself
 * accordingly.
 */
/* 
 * COPIED unmodified from org.eclipse.ui.views.navigator
 */
 
/* package */ abstract class FrameAction extends Action {
	private FrameList frameList;

	private IPropertyChangeListener propertyChangeListener = new IPropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent event) {
			FrameAction.this.handlePropertyChange(event);
		}
	};
	
/**
 * Creates a new frame action on the given frame list,
 * and hooks a property change listener on it.
 */
protected FrameAction(FrameList frameList) {
	this.frameList = frameList;
	frameList.addPropertyChangeListener(propertyChangeListener);
}
/**
 * Disposes this frame action.
 * The default implementation unhooks the property change listener from the frame list.
 */
public void dispose() {
	frameList.removePropertyChangeListener(propertyChangeListener);
}
/**
 * Returns the frame list.
 */
public FrameList getFrameList() {
	return frameList;
}
/**
 * Handles a property change event from the frame list.
 * The default implementation calls <code>update</code>.
 */
protected void handlePropertyChange(PropertyChangeEvent event) {
	update();
}
/**
 * Updates this action.  The default implementation calls <code>updateEnabledState</code>
 * and <code>updateToolTip</code>.
 */
public void update() {
	updateEnabledState();
	updateToolTip();
}
/**
 * Updates the enabled state of this action.
 * The default implementation does nothing.
 */
protected void updateEnabledState() {
}
/**
 * Updates the tool tip for this action.
 * The default implementation does nothing.
 */
protected void updateToolTip() {
}
}
