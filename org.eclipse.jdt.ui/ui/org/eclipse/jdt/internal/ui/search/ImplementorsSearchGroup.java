/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

/**
 * Contribute Java search specific menu elements.
 */
public class ImplementorsSearchGroup extends JavaSearchSubGroup  {

	public static final String GROUP_NAME= SearchMessages.getString("group.implementors"); //$NON-NLS-1$

	protected ElementSearchAction[] getActions() {
		return new ElementSearchAction[] {
			new FindImplementorsAction(),
			new FindImplementorsInWorkingSetAction()
		};
	}
	
	protected String getName() {
		return GROUP_NAME;
	}
}

