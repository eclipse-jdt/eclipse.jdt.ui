/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.ui.vcm;
 
import org.eclipse.core.resources.IResource;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaCore;import org.eclipse.jface.viewers.LabelProviderChangedEvent;import org.eclipse.swt.widgets.Shell;
import org.eclipse.vcm.internal.ui.VCMLabelDecorator;

/**
 * This label decorator adds Version information to resources.
 */
public class JavaVCMLabelDecorator extends VCMLabelDecorator {

	/**
	 * Creates a new decorator with the given shell.  The shell is
	 * needed for determining the UI display for updates.
	 */
	public JavaVCMLabelDecorator(Shell shell) {
		super(shell);
	}
	
	/**
 	 * Returns the change event to be fired for updates to the given resource.
 	 */
	protected LabelProviderChangedEvent createLabelEvent(IResource resource) {
		// check whether there is a corresponding Java resource
		IJavaElement element= JavaCore.create(resource);
		if (element != null)
			return new LabelProviderChangedEvent(this, element);
		return new LabelProviderChangedEvent(this, resource);
	}
 
	/*
	 * @see ILabelDecorator#decorateText(String, Object)
	 */
	public String decorateText(String text, Object o) {
		if (o instanceof IOpenable)
			return super.decorateText(text, o);
		return text;
	}
}