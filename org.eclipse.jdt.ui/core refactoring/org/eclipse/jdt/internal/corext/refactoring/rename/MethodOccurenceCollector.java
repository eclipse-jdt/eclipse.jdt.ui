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

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;

import org.eclipse.jdt.internal.corext.refactoring.CollectingSearchRequestor;
import org.eclipse.jdt.internal.corext.util.SearchUtils;
import org.eclipse.jdt.internal.corext.util.Strings;

final class MethodOccurenceCollector extends CollectingSearchRequestor {

		private final int fNameLength;

		public MethodOccurenceCollector(String methodName) {
			fNameLength= methodName.length();
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.refactoring.CollectingSearchRequestor#acceptSearchMatch(org.eclipse.jdt.core.search.SearchMatch)
		 */
		public void acceptSearchMatch(SearchMatch match) throws CoreException {
			ICompilationUnit unit= SearchUtils.getCompilationUnit(match);
			if (unit == null)
				return;
			
			IResource res= match.getResource();
			int accuracy= match.getAccuracy();
			int start= match.getOffset();
			IJavaElement element= (IJavaElement) match.getElement();
			
			String matchText= unit.getBuffer().getText(start, match.getLength());
			//TODO: use Scanner
			int leftBracketIndex= matchText.indexOf("("); //$NON-NLS-1$
			if (leftBracketIndex != -1) {
				// reference in code includes arguments; reference in javadoc doesn't; constructors ?
				matchText= matchText.substring(0, leftBracketIndex);
			}
		
			int theDotIndex= matchText.lastIndexOf("."); //$NON-NLS-1$
			if (theDotIndex == -1) {
				super.acceptSearchMatch(new SearchMatch(element, accuracy, start, fNameLength, SearchEngine.getDefaultSearchParticipant(), res));
			} else {
				start= start + theDotIndex + 1;
				for (int i= theDotIndex + 1; i < matchText.length() && Strings.isIndentChar(matchText.charAt(i)); i++) {
					start++;
				}
				super.acceptSearchMatch(new SearchMatch(element, accuracy, start, fNameLength, SearchEngine.getDefaultSearchParticipant(), res));
			}
		}	
	}
