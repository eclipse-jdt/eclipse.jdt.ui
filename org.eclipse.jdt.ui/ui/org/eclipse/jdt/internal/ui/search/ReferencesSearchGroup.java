/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

/**
 * Contribute Java search specific menu elements.
 */
public class ReferencesSearchGroup extends JavaSearchSubGroup  {

	public static final String GROUP_NAME= SearchMessages.getString("group.references"); //$NON-NLS-1$

	protected ElementSearchAction[] getActions() {
		return new ElementSearchAction[] {
			new FindReferencesAction(),			
			new FindReferencesInHierarchyAction(),
			new FindReferencesInWorkingSetAction(),
		};
	}
	
	protected String getName() {
		return GROUP_NAME;
	}
}
