/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

/**
 * Contribute Java search specific menu elements.
 */
public class ReadReferencesSearchGroup extends JavaSearchSubGroup  {

	public static final String GROUP_NAME= SearchMessages.getString("group.readReferences"); //$NON-NLS-1$

	protected ElementSearchAction[] getActions() {
		return new ElementSearchAction[] {
			new FindReadReferencesAction(),			
			new FindReadReferencesInHierarchyAction(),
			new FindReadReferencesInWorkingSetAction(),
		};
	}
	
	protected String getName() {
		return GROUP_NAME;
	}
}
