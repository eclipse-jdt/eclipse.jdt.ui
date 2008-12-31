/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ui.leaktest;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jdt.ui.leaktest.reftracker.ReferenceVisitor;
import org.eclipse.jdt.ui.leaktest.reftracker.ReferencedObject;

/**
 * An implementation of a {@link ReferenceVisitor} that collects all instances of a given type.
 * All matches are stored as {@link ReferenceInfo}
 */
public class InstancesOfTypeCollector extends ReferenceVisitor {

	private final ArrayList fResults;
	private int fMatchCount= 0;
	private long fInstanceCount= 0;
	private final boolean fIncludeSubtypes;
	private final String fRequestedTypeName;

	public InstancesOfTypeCollector(String requestedTypeName, boolean includeSubtypes) {
		fIncludeSubtypes= includeSubtypes;
		fResults= new ArrayList();

		fMatchCount= 0;

		fRequestedTypeName= requestedTypeName;
	}

	public ReferenceInfo[] getResults() {
		return (ReferenceInfo[]) fResults.toArray(new ReferenceInfo[fResults.size()]);
	}

	public String getResultString() {
		int i= 0;
		StringBuffer buf= new StringBuffer();
		for (Iterator iterator= fResults.iterator(); iterator.hasNext();) {
			ReferenceInfo element= (ReferenceInfo) iterator.next();
			buf.append("Element ").append(i++).append('\n');
			buf.append(element.toString()).append('\n');
		}
		return buf.toString();
	}

	public int getNumberOfResults() {
		return fResults.size();
	}

	public boolean visit(ReferencedObject reference, Class clazz, boolean firstVisit) {
		if (firstVisit) {
			fInstanceCount++;
			if (isMatchingType(clazz)) {
				fMatchCount++;
				fResults.add(new ReferenceInfo(reference));
			}
		}
		return true;
	}

	private boolean isMatchingType(Class clazz) {
		if (clazz.getName().equals(fRequestedTypeName)) {
			return true;
		}
		if (fIncludeSubtypes) {
			Class superclass= clazz.getSuperclass();
			if (superclass != null && isMatchingType(superclass)) {
				return true;
			}
			Class[] interfaces= clazz.getInterfaces();
			for (int i= 0; i < interfaces.length; i++) {
				if (isMatchingType(interfaces[i])) {
					return true;
				}
			}
		}
		return false;
	}

}
