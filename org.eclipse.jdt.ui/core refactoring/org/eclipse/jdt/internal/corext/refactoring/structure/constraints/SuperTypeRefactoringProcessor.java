/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.structure.constraints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine2;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ASTCreator;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.SearchUtils;

/**
 * Partial implementation of a refactoring processor solving supertype constraint models.
 * 
 * @since 3.1
 */
public abstract class SuperTypeRefactoringProcessor extends RefactoringProcessor {

	/** The working copy owner */
	protected final WorkingCopyOwner fOwner= new WorkingCopyOwner() {};

	/** The obsolete casts (element type: <code>&ltICompilationUnit, Collection&ltCastVariable2&gt&gt</code>) */
	protected Map fObsoleteCasts= null;

	/** The type matches (element type: <code>&ltICompilationUnit, Collection&ltIDeclaredConstraintVariable&gt</code>) */
	protected Map fTypeMatches= null;

	/**
	 * Computes the possible supertype changes to be made by the refactoring.
	 * 
	 * @param subType the subtype
	 * @param superType the supertype
	 * @param monitor the progress monitor to use
	 * @param status the refactoring status
	 * @throws JavaModelException if an error occurs
	 */
	protected final void computeSupertypeChanges(final IType subType, final IType superType, final IProgressMonitor monitor, final RefactoringStatus status) throws JavaModelException {
		Assert.isNotNull(subType);
		Assert.isNotNull(superType);
		Assert.isNotNull(monitor);
		Assert.isNotNull(status);
		final SuperTypeConstraintsModel model= new SuperTypeConstraintsModel();
		final SuperTypeConstraintsCreator creator= new SuperTypeConstraintsCreator(model);
		try {
			monitor.beginTask("", 2); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("SuperTypeRefactoringProcessor.creating")); //$NON-NLS-1$
			final Map map= getReferencedCompilationUnits(subType, superType, new SubProgressMonitor(monitor, 1), status);
			IJavaProject project= null;
			Collection collection= null;
			for (final Iterator iterator= map.keySet().iterator(); iterator.hasNext();) {
				project= (IJavaProject) iterator.next();
				collection= (Collection) map.get(project);
				if (collection != null) {
					final ASTParser parser= ASTParser.newParser(AST.JLS3);
					parser.setCompilerOptions(RefactoringASTParser.getCompilerOptions(project));
					parser.setResolveBindings(true);
					parser.setProject(project);
					parser.createASTs((ICompilationUnit[]) collection.toArray(new ICompilationUnit[collection.size()]), new String[0], new ASTRequestor() {

						public final void acceptAST(final ICompilationUnit unit, final CompilationUnit node) {
							monitor.subTask(unit.getElementName());
							node.setProperty(RefactoringASTParser.SOURCE_PROPERTY, unit);
							node.accept(creator);
						}

						public final void acceptBinding(final String key, final IBinding binding) {
							// Do nothing
						}
					}, new SubProgressMonitor(monitor, 1));
				}
			}
			final SuperTypeConstraintsSolver solver= new SuperTypeConstraintsSolver(model);
			solver.solveConstraints();
			fTypeMatches= solver.getTypeMatches();
			fObsoleteCasts= solver.getObsoleteCasts();
		} finally {
			monitor.done();
		}
	}

	/**
	 * Computes the compilation units referenced by replacing a subtype with a supertype.
	 * @param subType the subtype
	 * @param superType the supertype
	 * @param monitor the progress monitor to use
	 * @param status the refactoring status
	 * @return the referenced compilation units (element type: <code>&ltIJavaProject, Collection&ltICompilationUnit&gt&gt</code>)
	 * @throws JavaModelException if an error occurs
	 */
	protected final Map getReferencedCompilationUnits(final IType subType, final IType superType, final IProgressMonitor monitor, final RefactoringStatus status) throws JavaModelException {
		Assert.isNotNull(subType);
		Assert.isNotNull(superType);
		Assert.isNotNull(monitor);
		Assert.isNotNull(status);
		try {
			monitor.beginTask("", 2); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("SuperTypeRefactoringProcessor.creating")); //$NON-NLS-1$
			final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2();
			engine.setOwner(fOwner);
			engine.setFiltering(true, true);
			engine.setStatus(status);
			engine.setScope(RefactoringScopeFactory.create(subType));
			engine.setPattern(SearchPattern.createPattern(subType, IJavaSearchConstants.REFERENCES, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE));
			engine.searchPattern(new SubProgressMonitor(monitor, 1));
			
			SearchResultGroup[] typeReferences= null;
			ICompilationUnit[] typeReferencingCus= getCus(typeReferences);
	//		ICompilationUnit[] fieldAndMethodReferencingCus= fieldAndMethodReferringCus(subType, typeReferences, workingCopies, new SubProgressMonitor(monitor, 1), status);
	//		return merge(fieldAndMethodReferencingCus, typeReferencingCus);
			return null;
		} finally{
			monitor.done();
		}
	}

	private ICompilationUnit[] fieldAndMethodReferringCus(IType theType, SearchResultGroup[] typeReferences, ICompilationUnit[] wcs, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		SearchPattern pattern= RefactoringSearchEngine.createOrPattern(getReferencingFieldsAndMethods(typeReferences), IJavaSearchConstants.ALL_OCCURRENCES);
		if (pattern == null)
			return new ICompilationUnit[0];
		IJavaSearchScope scope= RefactoringScopeFactory.create(theType);
		ICompilationUnit[] units= RefactoringSearchEngine.findAffectedCompilationUnits(pattern, scope, pm, status);
		Set result= new HashSet(units.length);
		for (int i= 0; i < units.length; i++) {
			result.add(getUnproceededElement(units[i], wcs));
		}
		return (ICompilationUnit[]) result.toArray(new ICompilationUnit[result.size()]);
	}

	private static ICompilationUnit getUnproceededElement(ICompilationUnit unit, ICompilationUnit[] wcs) {
		if (wcs == null) 
			return unit;
		for (int i= 0; i < wcs.length; i++) {
			if (proceeds(wcs[i], unit))	
				return wcs[i];
		}
		return unit;
	}

	private static boolean proceeds(ICompilationUnit wc, ICompilationUnit unit) {
		return wc.getResource() == null || wc.getResource().equals(unit.getResource());			
	}

	private static ICompilationUnit[] getWorkingCopies(ICompilationUnit precedingWC1, ICompilationUnit precedingWC2) {
		ArrayList result= new ArrayList(2);
		if (precedingWC1 != null && precedingWC1.isWorkingCopy())
			result.add(precedingWC1);
		if (precedingWC2 != null && precedingWC2.isWorkingCopy())
			result.add(precedingWC2);
		return (ICompilationUnit[]) result.toArray(new ICompilationUnit[result.size()]);
	}

	private IMethod[] getReferencingMethods(ASTNode[] typeReferenceNodes) throws JavaModelException {
		List result= new ArrayList();
		for (int i= 0; i < typeReferenceNodes.length; i++) {
			ASTNode node= typeReferenceNodes[i];
			IJavaProject scope= ASTCreator.getCu(node).getJavaProject();
			IMethod method= getMethod(node, scope);
			if (method != null)
				result.add(method);
		}
		return (IMethod[]) result.toArray(new IMethod[result.size()]);
	}

	private IField[] getReferencingFields(ASTNode[] typeReferenceNodes) throws JavaModelException {
		List result= new ArrayList();
		for (int i= 0; i < typeReferenceNodes.length; i++) {
			ASTNode node= typeReferenceNodes[i];
			IJavaProject scope= ASTCreator.getCu(node).getJavaProject();
			result.addAll(Arrays.asList(getFields(node, scope)));
		}
		return (IField[]) result.toArray(new IField[result.size()]);
	}
	
	private IMember[] getReferencingFieldsAndMethods(SearchResultGroup[] typeReferences) throws JavaModelException {
		List result= new ArrayList();
		for (int i= 0; i < typeReferences.length; i++) {
			SearchResultGroup group= typeReferences[i];	
			ASTNode[] typeReferenceNodes= getAstNodes(group);
			result.addAll(Arrays.asList(getReferencingMethods(typeReferenceNodes)));
			result.addAll(Arrays.asList(getReferencingFields(typeReferenceNodes)));
		}
		return (IMember[]) result.toArray(new IMember[result.size()]);
	}

	private ASTNode[] getAstNodes(SearchResultGroup searchResultGroup) {
		ICompilationUnit cu= searchResultGroup.getCompilationUnit();
		if (cu == null)
			return new ASTNode[0];
		CompilationUnit cuNode= getAST(cu);
		return ASTNodeSearchUtil.getAstNodes(searchResultGroup.getSearchResults(), cuNode);
	}
	
	private CompilationUnit getAST(ICompilationUnit unit) {
		return ASTCreator.createAST(unit, fOwner);
	}

	private static IMethod getMethod(ASTNode node, IJavaProject scope) throws JavaModelException {
		if (node instanceof Type && node.getParent() instanceof MethodDeclaration){
			MethodDeclaration declaration= (MethodDeclaration)node.getParent();
			IMethodBinding binding= declaration.resolveBinding();
			if (binding != null)
				return Bindings.findMethod(binding, scope);
		} else if (node instanceof Type && isMethodParameter(node.getParent())){
			MethodDeclaration declaration= (MethodDeclaration)node.getParent().getParent();
			IMethodBinding binding= declaration.resolveBinding();
			if (binding != null)
				return Bindings.findMethod(binding, scope);
		}
		return null;
	}

	private static boolean isMethodParameter(ASTNode node){
		return (node instanceof VariableDeclaration) && 
			   (node.getParent() instanceof MethodDeclaration) &&
				((MethodDeclaration)node.getParent()).parameters().contains(node);
	}
	
	private static IField[] getFields(ASTNode node, IJavaProject scope) throws JavaModelException {
		if (node instanceof Type && node.getParent() instanceof FieldDeclaration){
			FieldDeclaration parent= (FieldDeclaration)node.getParent();
			if (parent.getType() == node){
				List result= new ArrayList(parent.fragments().size());
				for (Iterator iter= parent.fragments().iterator(); iter.hasNext();) {
					VariableDeclarationFragment fragment= (VariableDeclarationFragment) iter.next();
					IField field= getField(fragment, scope);
					if (field != null)
						result.add(field);
				}
				return (IField[]) result.toArray(new IField[result.size()]);
			}
		}  
		return new IField[0];
	}

	private static IField getField(VariableDeclarationFragment fragment, IJavaProject in) throws JavaModelException {
		IBinding binding= fragment.getName().resolveBinding();
		if (! (binding instanceof IVariableBinding))
			return null;
		IVariableBinding variableBinding= (IVariableBinding)binding;
		if (! variableBinding.isField())
			return null;
		return Bindings.findField(variableBinding, in);
	}

	private static ICompilationUnit[] merge(ICompilationUnit[] array1, ICompilationUnit[] array2){
		Set result= new HashSet();
		result.addAll(Arrays.asList(array1));
		result.addAll(Arrays.asList(array2));
		return (ICompilationUnit[]) result.toArray(new ICompilationUnit[result.size()]);
	}
	
	private static ICompilationUnit[] getCus(SearchResultGroup[] groups) {
		List result= new ArrayList(groups.length);
		for (int i= 0; i < groups.length; i++) {
			SearchResultGroup group= groups[i];
			ICompilationUnit cu= group.getCompilationUnit();
			if (cu != null)
				result.add(cu);
		}
		return (ICompilationUnit[]) result.toArray(new ICompilationUnit[result.size()]);
	}

}