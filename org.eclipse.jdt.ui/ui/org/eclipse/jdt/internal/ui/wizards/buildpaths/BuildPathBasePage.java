/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.List;

public abstract class BuildPathBasePage {
	
	public abstract List getSelection();
	public abstract void setSelection(List selection);
			
	protected void filterSelection(List list, int kind) {
		for (int i= list.size()-1; i >= 0; i--) {
			CPListElement curr= (CPListElement)list.get(i);
			if (curr.getEntryKind() != kind) {
				list.remove(i);
			}
		}
	}
	
}