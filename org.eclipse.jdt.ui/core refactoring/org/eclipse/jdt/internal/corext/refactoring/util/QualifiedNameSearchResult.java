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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;

import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;

public class QualifiedNameSearchResult {

	private Map fChanges;
	private List fChangesList;
	
	public QualifiedNameSearchResult() {
		fChanges= new HashMap();
		fChangesList= new ArrayList();
	}
	
	public TextChange getChange(IFile file) {
		TextChange result= (TextChange)fChanges.get(file);
		if (result == null) {
			result= new TextFileChange(file.getName(), file);
			fChanges.put(file, result);
			fChangesList.add(result);
		}
		return result;
	}
	
	public TextChange[] getAllChanges() {
		return (TextChange[])fChangesList.toArray(new TextChange[fChangesList.size()]);
	}
	
	public IFile[] getAllFiles() {
		Set keys= fChanges.keySet();
		return (IFile[])keys.toArray(new IFile[keys.size()]);			
	}
}
