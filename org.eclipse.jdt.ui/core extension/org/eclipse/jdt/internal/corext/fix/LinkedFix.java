/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;

import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

public class LinkedFix extends AbstractFix {
	
	public static abstract class AbstractLinkedFixRewriteOperation extends AbstractFixRewriteOperation implements ILinkedFixRewriteOperation {
		
		private Hashtable fPositionGroups;
		
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.fix.AbstractFix.IFixRewriteOperation#rewriteAST(org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite, java.util.List)
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups) throws CoreException {
			rewriteAST(cuRewrite, textEditGroups, new ArrayList());
		}

		protected PositionGroup getPositionGroup(String parameterName) {
			if (!fPositionGroups.containsKey(parameterName)) {
				fPositionGroups.put(parameterName, new PositionGroup(parameterName));
			}
			return (PositionGroup)fPositionGroups.get(parameterName);
		}
		
		protected void clearPositionGroups() {
			if (fPositionGroups == null) {
				fPositionGroups= new Hashtable();
			} else {
				fPositionGroups.clear();
			}
		}
		
		protected Collection getAllPositionGroups() {
			if (fPositionGroups == null)
				return null;
			
			return fPositionGroups.values();
		}
	}
	
	private final IFixRewriteOperation[] fFixRewrites;
	private final CompilationUnit fCompilationUnit;
	private final List fPositionGroups;
	private ITrackedNodePosition fEndPosition;

	protected LinkedFix(String name, CompilationUnit compilationUnit, IFixRewriteOperation[] fixRewrites) {
		super(name, compilationUnit, null);
		fCompilationUnit= compilationUnit;
		fFixRewrites= fixRewrites;
		fPositionGroups= new ArrayList();
	}
	
	public ITrackedNodePosition getEndPosition() {
		return fEndPosition;
	}
	
	public PositionGroup[] getPositionGroups() {
		return (PositionGroup[])fPositionGroups.toArray(new PositionGroup[fPositionGroups.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.fix.IFix#createChange()
	 */
	public TextChange createChange() throws CoreException {
		if (fFixRewrites == null || fFixRewrites.length == 0)
			return null;

		CompilationUnitRewrite cuRewrite= new CompilationUnitRewrite((ICompilationUnit)fCompilationUnit.getJavaElement(), fCompilationUnit);
	
		List/*<TextEditGroup>*/ groups= new ArrayList();

		fEndPosition= null;
		fPositionGroups.clear();
		
		for (int i= 0; i < fFixRewrites.length; i++) {
			IFixRewriteOperation operation= fFixRewrites[i];
			if (operation instanceof ILinkedFixRewriteOperation) {
				ILinkedFixRewriteOperation linkedOperation= (ILinkedFixRewriteOperation)operation;
				fEndPosition= linkedOperation.rewriteAST(cuRewrite, groups, fPositionGroups);
			} else {
				operation.rewriteAST(cuRewrite, groups);
			}
		}
		
		CompilationUnitChange result= cuRewrite.createChange();
		
		for (Iterator iter= groups.iterator(); iter.hasNext();) {
			TextEditGroup group= (TextEditGroup)iter.next();
			result.addTextEditGroup(group);
		}
		return result;
	}
}
