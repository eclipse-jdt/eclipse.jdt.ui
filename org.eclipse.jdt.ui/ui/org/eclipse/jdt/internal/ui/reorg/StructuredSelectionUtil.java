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
package org.eclipse.jdt.internal.ui.reorg;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;

class StructuredSelectionUtil {
	
	private static final IResource[] EMPTY= new IResource[0];
	
	private StructuredSelectionUtil(){
	}
	
	static boolean hasNonResources(IStructuredSelection ss) {
		for (Iterator iter= ss.iterator(); iter.hasNext();) {
			if (ResourceUtil.getResource(iter.next()) == null)
				return true;	
		}
		return false;
	}

	static IResource[] getResources(IStructuredSelection ss){
		if (ss == null || ss.isEmpty())
			return EMPTY;
		List selectedResources= getResourceList(ss);
		return ((IResource[]) selectedResources.toArray(new IResource[selectedResources.size()]));		
	}

	private static List getResourceList(IStructuredSelection ss) {
		List result= new ArrayList(0);
		for (Iterator iter= ss.iterator(); iter.hasNext();) {
			IResource resource= ResourceUtil.getResource(iter.next());
			if (resource != null)
				result.add(resource);
		}
		return result;
	}
}
