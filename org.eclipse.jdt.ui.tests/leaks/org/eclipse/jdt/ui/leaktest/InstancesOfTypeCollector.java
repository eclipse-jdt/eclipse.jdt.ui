/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ui.leaktest;

import java.util.ArrayList;

import org.eclipse.jdt.ui.leaktest.reftracker.ReferenceVisitor;
import org.eclipse.jdt.ui.leaktest.reftracker.ReferencedObject;

/**
 * An implementation of a {@link ReferenceVisitor} that collects all instances of a given type.
 * All matches are stored as {@link ReferenceInfo}
 */
public class InstancesOfTypeCollector extends ReferenceVisitor {

	private final ArrayList<ReferenceInfo> fResults;
	private final boolean fIncludeSubtypes;
	private final String fRequestedTypeName;

	public InstancesOfTypeCollector(String requestedTypeName, boolean includeSubtypes) {
		fIncludeSubtypes= includeSubtypes;
		fResults= new ArrayList<>();
		fRequestedTypeName= requestedTypeName;
	}

	public ReferenceInfo[] getResults() {
		return fResults.toArray(new ReferenceInfo[fResults.size()]);
	}

	public String getResultString() {
		int i= 0;
		StringBuilder buf= new StringBuilder();
		for (ReferenceInfo element : fResults) {
			buf.append("Element ").append(i++).append('\n');
			buf.append(element.toString()).append('\n');
		}
		return buf.toString();
	}

	public int getNumberOfResults() {
		return fResults.size();
	}

	@Override
	public boolean visit(ReferencedObject reference, Class<?> clazz, boolean firstVisit) {
		if (firstVisit) {
			if (isMatchingType(clazz)) {
				fResults.add(new ReferenceInfo(reference));
			}
		}
		return true;
	}

	private boolean isMatchingType(Class<?> clazz) {
		if (clazz.getName().equals(fRequestedTypeName)) {
			return true;
		}
		if (fIncludeSubtypes) {
			Class<?> superclass= clazz.getSuperclass();
			if (superclass != null && isMatchingType(superclass)) {
				return true;
			}
			for (Class<?> intf : clazz.getInterfaces()) {
				if (isMatchingType(intf)) {
					return true;
				}
			}
		}
		return false;
	}

}
