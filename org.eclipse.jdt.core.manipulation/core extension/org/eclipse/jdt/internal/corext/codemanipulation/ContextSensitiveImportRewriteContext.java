/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *     Red Hat Inc. - moved to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;
import org.eclipse.jdt.core.manipulation.ImportReferencesCollector;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;

import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

/**
 * This <code>ImportRewriteContext</code> is aware of all the types visible in
 * <code>compilationUnit</code> at <code>position</code>.
 * <p>
 * <b>Note:</b> This context only works if the AST was created with bindings!
 * </p>
 */
public class ContextSensitiveImportRewriteContext extends ImportRewriteContext {

	private final CompilationUnit fCompilationUnit;
	private final int fPosition;
	private IBinding[] fDeclarationsInScope;
	private Name[] fImportedNames;
	private final ImportRewrite fImportRewrite;
	private RedundantNullnessTypeAnnotationsFilter fRedundantTypeAnnotationsFilter;

	/**
	 * Creates an import rewrite context at the given node's start position.
	 *
	 * @param node the node to use as context
	 * @param importRewrite the import rewrite
	 *
	 * @since 3.6
	 */
	public ContextSensitiveImportRewriteContext(ASTNode node, ImportRewrite importRewrite) {
		this((CompilationUnit) node.getRoot(), node.getStartPosition(), importRewrite, RedundantNullnessTypeAnnotationsFilter.createIfConfigured(node));
	}

	/**
	 * Creates an import rewrite context at the given start position.
	 *
	 * @param compilationUnit the root (must have resolved bindings)
	 * @param position the context position
	 * @param importRewrite the import rewrite
	 */
	public ContextSensitiveImportRewriteContext(CompilationUnit compilationUnit, int position, ImportRewrite importRewrite) {
		this(compilationUnit, position, importRewrite, RedundantNullnessTypeAnnotationsFilter.createIfConfigured(new NodeFinder(compilationUnit, position, 0).getCoveringNode()));
	}

	private ContextSensitiveImportRewriteContext(CompilationUnit compilationUnit, int position, ImportRewrite importRewrite,
			RedundantNullnessTypeAnnotationsFilter redundantNullnessTypeAnnotationsFilter) {
		fCompilationUnit= compilationUnit;
		fPosition= position;
		fImportRewrite= importRewrite;
		fDeclarationsInScope= null;
		fImportedNames= null;
		fRedundantTypeAnnotationsFilter= redundantNullnessTypeAnnotationsFilter;
	}

	@Override
	public int findInContext(String qualifier, String name, int kind) {
		for (IBinding declaration : getDeclarationsInScope()) {
			if (declaration instanceof ITypeBinding) {
				ITypeBinding typeBinding= (ITypeBinding) declaration;
				if (isSameType(typeBinding, qualifier, name)) {
					return RES_NAME_FOUND;
				} else if (isConflicting(typeBinding, name)) {
					return RES_NAME_CONFLICT;
				}
			} else if (declaration != null) {
				if (isConflicting(declaration, name)) {
					return RES_NAME_CONFLICT;
				}
			}
		}


		for (Name importedName : getImportedNames()) {
			IBinding binding= importedName.resolveBinding();
			if (binding instanceof ITypeBinding && !binding.isRecovered()) {
				ITypeBinding typeBinding= (ITypeBinding)binding;
				if (isConflictingType(typeBinding, qualifier, name)) {
					return RES_NAME_CONFLICT;
				}
			}
		}

		List<AbstractTypeDeclaration> list= fCompilationUnit.types();
		for (AbstractTypeDeclaration type : list) {
			ITypeBinding binding= type.resolveBinding();
			if (binding != null) {
				if (isSameType(binding, qualifier, name)) {
					return RES_NAME_FOUND;
				} else {
					ITypeBinding decl= containingDeclaration(binding, qualifier, name);
					while (decl != null && !decl.equals(binding)) {
						int modifiers= decl.getModifiers();
						if (Modifier.isPrivate(modifiers))
							return RES_NAME_CONFLICT;
						decl= decl.getDeclaringClass();
					}
				}
			}
		}

		String qualifiedName= JavaModelUtil.concatenateName(qualifier, name);
		for (String addedImport : fImportRewrite.getAddedImports()) {
			if (qualifiedName.equals(addedImport)) {
				return RES_NAME_FOUND;
			} else {
				if (isConflicting(name, addedImport))
					return RES_NAME_CONFLICT;
			}
		}

		if ("java.lang".equals(qualifier)) { //$NON-NLS-1$
			//No explicit import statement required
			ITypeRoot typeRoot= fCompilationUnit.getTypeRoot();
			if (typeRoot != null) {
				IPackageFragment packageFragment= (IPackageFragment) typeRoot.getParent();
				try {
					for (ICompilationUnit cu : packageFragment.getCompilationUnits()) {
						for (IType type : cu.getAllTypes()) {
							String packageTypeName= type.getFullyQualifiedName();
							if (isConflicting(name, packageTypeName))
								return RES_NAME_CONFLICT;
						}
					}
				} catch (JavaModelException e) {
				}
			}
		}

		return fImportRewrite.getDefaultImportRewriteContext().findInContext(qualifier, name, kind);
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

	private ITypeBinding containingDeclaration(ITypeBinding binding, String qualifier, String name) {
		for (ITypeBinding childBinding : binding.getDeclaredTypes()) {
			if (isSameType(childBinding, qualifier, name)) {
				return childBinding;
			} else {
				ITypeBinding result= containingDeclaration(childBinding, qualifier, name);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
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

			List<SimpleName> imports= new ArrayList<>();
			ImportReferencesCollector.collect(fCompilationUnit, project, null, imports, null);
			fImportedNames= imports.toArray(new Name[imports.size()]);
		}
		return fImportedNames;
	}

	@Override
	public IAnnotationBinding[] removeRedundantTypeAnnotations(IAnnotationBinding[] annotations, TypeLocation location, ITypeBinding type) {
		RedundantNullnessTypeAnnotationsFilter redundantTypeAnnotationsFilter= fRedundantTypeAnnotationsFilter;
		if (redundantTypeAnnotationsFilter != null) {
			annotations= redundantTypeAnnotationsFilter.removeUnwantedTypeAnnotations(annotations, location, type);
		}
		return super.removeRedundantTypeAnnotations(annotations, location, type);
	}
}
