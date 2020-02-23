/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Microsoft Corporation - copied to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.nls;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;


public class NLSLine {

	private int fLineNumber;
	private List<NLSElement> fElements;

	public NLSLine(int lineNumber) {
		fLineNumber= lineNumber;
		Assert.isTrue(fLineNumber >= 0);
		fElements= new ArrayList<>();
	}

	public int getLineNumber() {
		return fLineNumber;
	}

	/**
	 * Adds a NLS element to this line.
	 *
	 * @param element the NLS element
	 */
	public void add(NLSElement element) {
		Assert.isNotNull(element);
		fElements.add(element);
	}

	public NLSElement[] getElements() {
		return fElements.toArray(new NLSElement[fElements.size()]);
	}

	public NLSElement get(int index) {
		return fElements.get(index);
	}

	public boolean exists(int index) {
		return index >= 0 && index < fElements.size();
	}

	public int size(){
		return fElements.size();
	}

	/* non javaDoc
	 * only for debugging
	 * @see Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder result= new StringBuilder();
		result.append("Line: " + fLineNumber + "\n"); //$NON-NLS-2$ //$NON-NLS-1$
		for (NLSElement nlsElement : fElements) {
			result.append("\t"); //$NON-NLS-1$
			result.append(nlsElement.toString());
			result.append("\n"); //$NON-NLS-1$
		}
		return result.toString();
	}
}

