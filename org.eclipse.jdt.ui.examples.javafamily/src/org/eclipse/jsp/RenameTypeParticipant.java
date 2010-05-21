/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jsp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.text.edits.ReplaceEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.core.indexsearch.ISearchResultCollector;

import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;


public class RenameTypeParticipant extends RenameParticipant {

	private IType fType;

	protected boolean initialize(Object element) {
		fType= (IType)element;
		return true;
	}

	public String getName() {
		return JspMessages.RenameTypeParticipant_name; 
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IRenameParticipant#checkInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context) {
		return new RefactoringStatus();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IRenameParticipant#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException {
		final Map changes= new HashMap();
		final String newName= computeNewName();
		ISearchResultCollector collector= new ISearchResultCollector() {
			public void accept(IResource resource, int start, int length) throws CoreException {
				TextFileChange change= (TextFileChange)changes.get(resource);
				if (change == null) {
					change= new TextFileChange(resource.getName(), (IFile)resource);
					changes.put(resource, change);
				}
				TextChangeCompatibility.addTextEdit(change, "Update type reference", new ReplaceEdit(start, length, newName)); //$NON-NLS-1$
			}
		};
		JspUIPlugin.getDefault().search(new JspTypeQuery(fType), collector, pm);
		
		if (changes.size() == 0)
			return null;
		CompositeChange result= new CompositeChange("JSP updates"); //$NON-NLS-1$
		for (Iterator iter= changes.values().iterator(); iter.hasNext();) {
			result.add((Change)iter.next());
		}
		return result;
	}
	
	private String computeNewName() {
		String newName= getArguments().getNewName();
		String currentName= fType.getFullyQualifiedName();
		int pos= currentName.lastIndexOf('.');
		if (pos == -1)
			return newName;
		return currentName.substring(0, pos + 1) + newName;
	}

}
