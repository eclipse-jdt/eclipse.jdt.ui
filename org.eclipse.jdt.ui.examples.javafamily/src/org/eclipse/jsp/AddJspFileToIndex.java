/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jsp;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.internal.compiler.util.Util;
import org.eclipse.jdt.internal.core.index.IIndex;
import org.eclipse.jdt.internal.core.index.impl.IFileDocument;
import org.eclipse.jdt.internal.core.search.indexing.IndexManager;
import org.eclipse.jsp.copied_from_jdtcore.AddFileToIndex;

/**
 */
class AddJspFileToIndex extends AddFileToIndex {

	char[] fContents;

	public AddJspFileToIndex(IFile resource, IPath indexPath0, IndexManager manager0) {
		super(resource, indexPath0, manager0);
	}
		
	protected boolean indexDocument(IIndex index) throws IOException {
		System.out.println("AddFileToIndex.indexDocument: " + getResource()); //$NON-NLS-1$
		if (! initializeContents() ) 
			return false;
		index.add(new IFileDocument(getResource(), fContents), new JspSourceIndexer());
		return true;
	}

	public boolean initializeContents() {
		if (fContents == null) {
			try {
				IPath location= getResource().getLocation();
				if (location != null)
					fContents= Util.getFileCharContent(location.toFile(), null);
			} catch (IOException e) {
				JspUIPlugin.log("internal error", e); //$NON-NLS-1$
			}
		}
		return fContents != null;
	}
}
