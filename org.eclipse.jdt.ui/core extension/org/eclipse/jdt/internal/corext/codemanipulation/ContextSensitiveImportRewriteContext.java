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
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;

import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

/**
 * This {@link ImportRewriteContext} is aware of all the types visible in 
 * <code>compilationUnit</code> at <code>position</code>.
 */
public class ContextSensitiveImportRewriteContext extends ImportRewriteContext {
	
	private final CompilationUnit fCompilationUnit;
	private final int fPosition;
	private IBinding[] fDeclarationsInScope;
	private Name[] fImportedNames;
	private final ImportRewrite fImportRewrite;
	
	public ContextSensitiveImportRewriteContext(CompilationUnit compilationUnit, int position, ImportRewrite importRewrite) {
		fCompilationUnit= compilationUnit;
		fPosition= position;
		fImportRewrite= importRewrite;
		fDeclarationsInScope= null;
		fImportedNames= null;
	}

	public int findInContext(String qualifier, String name, int kind) {
		IBinding[] declarationsInScope= getDeclarationsInScope();
		for (int i= 0; i < declarationsInScope.length; i++) {
			if (declarationsInScope[i] instanceof ITypeBinding) {
				ITypeBinding typeBinding= (ITypeBinding)declarationsInScope[i];
				if (isSameType(typeBinding, qualifier, name)) {
					return RES_NAME_FOUND;
				} else if (isConflicting(typeBinding, name)) {
					return RES_NAME_CONFLICT;
				}
			} else if (declarationsInScope[i] != null) {
				if (isConflicting(declarationsInScope[i], name)) {
					return RES_NAME_CONFLICT;
				}
			}
		}
		
		Name[] names= getImportedNames();
		for (int i= 0; i < names.length; i++) {
			IBinding binding= names[i].resolveBinding();
			if (binding instanceof ITypeBinding) {
				ITypeBinding typeBinding= (ITypeBinding)binding;
				if (isConflictingType(typeBinding, qualifier, name)) {
					return RES_NAME_CONFLICT;
				}
			}
		}
		
		List list= fCompilationUnit.types();
		for (Iterator iter= list.iterator(); iter.hasNext();) {
			AbstractTypeDeclaration type= (AbstractTypeDeclaration)iter.next();
			ITypeBinding binding= type.resolveBinding();
			if (isSameType(binding, qualifier, name)) {
				return RES_NAME_FOUND;
			} else {
				if (containsDeclaration(binding, qualifier, name))
					return RES_NAME_CONFLICT;
			}
		}
		
		String[] addedImports= fImportRewrite.getAddedImports();
		String qualifiedName= JavaModelUtil.concatenateName(qualifier, name);
		for (int i= 0; i < addedImports.length; i++) {
			String addedImport= addedImports[i];
			if (qualifiedName.equals(addedImport)) {
				return RES_NAME_FOUND;
			} else {
				if (isConflicting(name, addedImport))
					return RES_NAME_CONFLICT;
			}
		}
		
		if (qualifier.equals("java.lang")) { //$NON-NLS-1$
			//No explicit import statement required
			IJavaElement parent= fCompilationUnit.getJavaElement().getParent();
			if (parent instanceof IPackageFragment) {
				IPackageFragment packageFragment= (IPackageFragment)parent;
				try {
					ICompilationUnit[] compilationUnits= packageFragment.getCompilationUnits();
					for (int i= 0; i < compilationUnits.length; i++) {
						ICompilationUnit cu= compilationUnits[i];
						IType[] allTypes= cu.getAllTypes();
						for (int j= 0; j < allTypes.length; j++) {
							IType type= allTypes[j];
							String packageTypeName= type.getFullyQualifiedName();
							if (isConflicting(name, packageTypeName))
								return RES_NAME_CONFLICT;
						}
					}
				} catch (JavaModelException e) {
				}
			}
		}
		
		return RES_NAME_UNKNOWN;
	}

	private boolean isConflicting(String name, String importt) {
		int index= importt.lastIndexOf('.');
		String importedName;
		if (index == -1) {
			importedName= importt;
		} else {
			importedName= importt.substring(index + 1, importt.length());
		}
		if (importedName.equals(name)) {
			return true;
		}
		return false;
	}
	
	private boolean containsDeclaration(ITypeBinding binding, String qualifier, String name) {
		ITypeBinding[] declaredTypes= binding.getDeclaredTypes();
		for (int i= 0; i < declaredTypes.length; i++) {
			ITypeBinding childBinding= declaredTypes[i];
			if (isSameType(childBinding, qualifier, name)) {
				return true;
			} else {
				if (containsDeclaration(childBinding, qualifier, name)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isConflicting(IBinding binding, String name) {
		return binding.getName().equals(name);
	}

	private boolean isSameType(ITypeBinding binding, String qualifier, String name) {
		String qualifiedName= JavaModelUtil.concatenateName(qualifier, name);
		return binding.getQualifiedName().equals(qualifiedName);
	}
	
	private boolean isConflictingType(ITypeBinding binding, String qualifier, String name) {
		binding= binding.getTypeDeclaration();
		return !isSameType(binding, qualifier, name) && isConflicting(binding, name);
	}
	
	private IBinding[] getDeclarationsInScope() {
		if (fDeclarationsInScope == null) {
			ScopeAnalyzer analyzer= new ScopeAnalyzer(fCompilationUnit);
			fDeclarationsInScope= analyzer.getDeclarationsInScope(fPosition, ScopeAnalyzer.METHODS | ScopeAnalyzer.TYPES | ScopeAnalyzer.VARIABLES);
		}
		return fDeclarationsInScope;
	}
	
	private Name[] getImportedNames() {
		if (fImportedNames == null) {
			IJavaProject project= null;
			IJavaElement javaElement= fCompilationUnit.getJavaElement();
			if (javaElement != null)
				project= javaElement.getJavaProject();
			
			List imports= new ArrayList();
			ImportReferencesCollector.collect(fCompilationUnit, project, null, imports, null);
			fImportedNames= (Name[])imports.toArray(new Name[imports.size()]);
		}
		return fImportedNames;
	}
}