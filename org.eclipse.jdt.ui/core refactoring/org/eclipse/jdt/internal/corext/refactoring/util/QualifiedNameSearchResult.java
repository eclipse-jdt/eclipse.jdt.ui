/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;

public class QualifiedNameSearchResult {

	private Map fChanges;
	
	public QualifiedNameSearchResult() {
		fChanges= new HashMap();
	}
	
	public TextChange getChange(IFile file) {
		TextChange result= (TextChange)fChanges.get(file);
		if (result == null) {
			result= new TextFileChange(file.getName(), file);
			fChanges.put(file, result);
		}
		return result;
	}
	
	public TextChange[] getAllChanges() {
		Collection values= fChanges.values();
		return (TextChange[])values.toArray(new TextChange[values.size()]);
	}
	
	public IFile[] getAllFiles() {
		Set keys= fChanges.keySet();
		return (IFile[])keys.toArray(new IFile[keys.size()]);			
	}

	public Change getSingleChange(IFile[] alreadyTouchedFiles) {
		Collection values= fChanges.values();
		if (values.size() == 0)
			return null;
		
		CompositeChange result= new CompositeChange(RefactoringCoreMessages.getString("QualifiedNameSearchResult.change_name")); //$NON-NLS-1$
		List files= Arrays.asList(alreadyTouchedFiles);
		for (Iterator iter= values.iterator(); iter.hasNext();) {
			TextFileChange change= (TextFileChange)iter.next();
			if (!files.contains(change.getFile())) {
				result.add(change);
			}
		}
		return result;
	}
}
