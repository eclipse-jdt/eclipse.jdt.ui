/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.IClasspathEntry;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public abstract class BuildPathBasePage {
	
	public abstract List getSelection();
	public abstract void setSelection(List selection);
	
	public abstract boolean isEntryKind(int kind);
			
	protected void filterAndSetSelection(List list) {
		ArrayList res= new ArrayList(list.size());
		for (int i= list.size()-1; i >= 0; i--) {
			Object curr= list.get(i);
			if (curr instanceof CPListElement) {
				CPListElement elem= (CPListElement) curr;
				if (elem.getParentContainer() == null && isEntryKind(elem.getEntryKind())) {
					res.add(curr);
				}
			}
		}
		setSelection(res);
	}
	
	protected void fixNestingConflicts(List newEntries, List existing, Set modifiedSourceEntries) {
		for (int i= 0; i < newEntries.size(); i++) {
			CPListElement curr= (CPListElement) newEntries.get(i);
			addExclusionPatterns(curr, existing, modifiedSourceEntries);
		}
	}
	
	private void addExclusionPatterns(CPListElement newEntry, List existing, Set modifiedEntries) {
		IPath entryPath= newEntry.getPath();
		for (int i= 0; i < existing.size(); i++) {
			CPListElement curr= (CPListElement) existing.get(i);
			if (curr.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				IPath currPath= curr.getPath();
				if (currPath.isPrefixOf(entryPath) && !currPath.equals(entryPath)) {
					IPath[] exclusionFilters= (IPath[]) curr.getAttribute(CPListElement.EXCLUSION);
					if (!JavaModelUtil.isExcludedPath(entryPath, exclusionFilters)) {
						IPath pathToExclude= entryPath.removeFirstSegments(currPath.segmentCount()).addTrailingSeparator();
						IPath[] newExclusionFilters= new IPath[exclusionFilters.length + 1];
						System.arraycopy(exclusionFilters, 0, newExclusionFilters, 0, exclusionFilters.length);
						newExclusionFilters[exclusionFilters.length]= pathToExclude;
						curr.setAttribute(CPListElement.EXCLUSION, newExclusionFilters);
						modifiedEntries.add(curr);
					}
				}
			}
		}
	}
	
	protected boolean hasAttributes(List selElements) {
		if (selElements.size() == 0) {
			return false;
		}
		for (int i= 0; i < selElements.size(); i++) {
			if (selElements.get(i) instanceof CPListElementAttribute) {
				return true;
			}
		}
		return false;
	}
	
}
