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
package org.eclipse.jdt.internal.corext.refactoring.rename;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultCollector;
import org.eclipse.jdt.internal.corext.util.Strings;

final class MethodOccurenceCollector extends SearchResultCollector {

		private final int fNameLength;

		public MethodOccurenceCollector(IProgressMonitor pm, String methodName) {
			super(pm);
			fNameLength= methodName.length();
		}

		public void accept(IResource res, int start, int end, IJavaElement element, int accuracy) throws CoreException {
			ICompilationUnit unit= (ICompilationUnit)element.getAncestor(IJavaElement.COMPILATION_UNIT);
			if (unit == null)
				return;
			int matchLength= end - start;
			IBuffer buffer= unit.getBuffer();
			String match= buffer.getText(start, matchLength);
			//TODO: use Scanner
			int leftBracketIndex= match.indexOf("("); //$NON-NLS-1$
			if (leftBracketIndex != -1) {
				// reference in code includes arguments; reference in javadoc doesn't; constructors ?
				end= start + leftBracketIndex;
				match= match.substring(0, leftBracketIndex);
			}
		
			int theDotIndex= match.lastIndexOf("."); //$NON-NLS-1$
			if (theDotIndex == -1) {
				getResults().add(new SearchResult(res, start, start + fNameLength, element, accuracy));
			} else {
				start= start + theDotIndex + 1;
				for (int i= theDotIndex + 1; i < match.length() && Strings.isIndentChar(match.charAt(i)); i++) {
					start++;
				}
				getResults().add(new SearchResult(res, start, start + fNameLength, element, accuracy));
			}			
		}		
	}
