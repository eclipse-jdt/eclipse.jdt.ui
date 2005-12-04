/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
import java.util.Iterator;
import java.util.List;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportReferencesCollector;
import org.eclipse.jdt.internal.corext.codemanipulation.NewImportRewrite;
import org.eclipse.jdt.internal.corext.codemanipulation.NewImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;


public abstract class AbstractFix implements IFix {
	
	public interface IFixRewriteOperation {
		public void rewriteAST(
				ASTRewrite rewrite, 
				NewImportRewrite importRewrite, 
				CompilationUnit compilationUnit,
				List/*<TextEditGroup>*/ textEditGroups) throws CoreException; //TODO: ma maybe passing in a CompilationUnitRewrite would be easier?
	}
	
	public static abstract class AbstractFixRewriteOperation implements IFixRewriteOperation {
		
		private static class ContextSensitiveImportRewriteContext extends ImportRewriteContext {
			
			private final CompilationUnit fCompilationUnit;
			private final ITypeBinding fImport;
			private final ASTNode fAccessor;
			
			public ContextSensitiveImportRewriteContext(CompilationUnit compilationUnit, ITypeBinding impord, ASTNode accessor) {
				fCompilationUnit= compilationUnit;
				fImport= impord; 
				fAccessor= accessor;
			}

			public int findInContext(String qualifier, final String name, int kind) {
				
				ScopeAnalyzer analyzer= new ScopeAnalyzer(fCompilationUnit);
				IBinding[] declarationsInScope= analyzer.getDeclarationsInScope(fAccessor.getStartPosition(), ScopeAnalyzer.TYPES);
				for (int i= 0; i < declarationsInScope.length; i++) {
					if (declarationsInScope[i] == fImport) { // TODO: ma: Bug should use qualifier and name, fImport is unnecessary
						return RES_NAME_FOUND;
					}
				}
				
				List imports= new ArrayList();
				List staticImports= new ArrayList(); // TODO: ma: can set to null if not interested in staticImports
				ImportReferencesCollector.collect(fCompilationUnit, fCompilationUnit.getJavaElement().getJavaProject(), null, imports, staticImports);
				for (Iterator iter= imports.iterator(); iter.hasNext();) {
					Name element= (Name)iter.next();
					IBinding binding= element.resolveBinding();
					if (binding instanceof ITypeBinding) {
						ITypeBinding declBinding= ((ITypeBinding)binding).getDeclaringClass();
						if (declBinding == null)
							declBinding= (ITypeBinding)binding;
						
						// TODO: ma: be careful with declBinding.getName(): will contain <> for parameterized types
						if (binding != fImport && declBinding.getName().equals(fImport.getName())) {
							return RES_NAME_CONFLICT;
						}
					}
				}
				
				return RES_NAME_UNKNOWN;
			}
		}
		
		// TODO: ma: better name importType
		protected Type importClass(final ITypeBinding toImport, final ASTNode accessor, NewImportRewrite imports, final CompilationUnit compilationUnit) {
			imports.setFilterImplicitImports(true);
			ImportRewriteContext importContext= new ContextSensitiveImportRewriteContext(compilationUnit, toImport, accessor);
			return imports.addImport(toImport, compilationUnit.getAST(), importContext);
		}
	}
	
	private final String fName;
	private final ICompilationUnit fCompilationUnit;
	private final IFixRewriteOperation[] fFixRewrites;
	private final CompilationUnit fUnit;
	
	protected AbstractFix(String name, CompilationUnit compilationUnit, IFixRewriteOperation[] fixRewriteOperations) {
		fName= name;
		fCompilationUnit= (ICompilationUnit)compilationUnit.getJavaElement();
		fFixRewrites= fixRewriteOperations;
		fUnit= compilationUnit;
	}

	/**
	 * @deprecated
	 */
	public AbstractFix(String name, ICompilationUnit compilationUnit) {
		fName= name;
		fCompilationUnit= compilationUnit;
		fFixRewrites= null;
		fUnit= null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.fix.IFix#getName()
	 */
	public String getDescription() {
		return fName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.fix.IFix#getCompilationUnit()
	 */
	public ICompilationUnit getCompilationUnit() {
		return fCompilationUnit;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.fix.IFix#createChange()
	 */
	public TextChange createChange() throws CoreException {
		if (fFixRewrites == null || fFixRewrites.length == 0)
			return null;

		CompilationUnitRewrite cuRewrite= new CompilationUnitRewrite(getCompilationUnit(), fUnit);
	
		ASTRewrite rewrite= cuRewrite.getASTRewrite();
		NewImportRewrite importRewrite= cuRewrite.getImportRewrite().getNewImportRewrite();
		
		List/*<TextEditGroup>*/ groups= new ArrayList();
		
		for (int i= 0; i < fFixRewrites.length; i++) {
			fFixRewrites[i].rewriteAST(rewrite, importRewrite, fUnit, groups);
		}
		
		CompilationUnitChange result= cuRewrite.createChange();
		
		for (Iterator iter= groups.iterator(); iter.hasNext();) {
			TextEditGroup group= (TextEditGroup)iter.next();
			result.addTextEditGroup(group);
		}
		return result;
	}

}
