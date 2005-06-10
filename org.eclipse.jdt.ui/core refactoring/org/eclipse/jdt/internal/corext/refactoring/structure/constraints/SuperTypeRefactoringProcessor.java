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
package org.eclipse.jdt.internal.corext.refactoring.structure.constraints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;

import org.eclipse.jdt.core.BindingKey;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
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
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine2;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.CompilationUnitRange;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TType;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TypeEnvironment;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.SearchUtils;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Partial implementation of a refactoring processor solving supertype constraint models.
 * 
 * @since 3.1
 */
public abstract class SuperTypeRefactoringProcessor extends RefactoringProcessor {

	/** Number of compilation units to parse at once */
	private static final int SIZE_BATCH= 500;

	/**
	 * Returns a new ast node corresponding to the given type.
	 * 
	 * @param rewrite the compilation unit rewrite to use
	 * @param type the specified type
	 * @return A corresponding ast node
	 */
	protected static ASTNode createCorrespondingNode(final CompilationUnitRewrite rewrite, final TType type) {
		return rewrite.getImportRewrite().addImportFromSignature(new BindingKey(type.getBindingKey()).internalToSignature(), rewrite.getAST());
	}

	/** Should type occurrences on instanceof's also be rewritten? */
	protected boolean fInstanceOf= false;

	/** The obsolete casts (element type: <code>&ltICompilationUnit, Collection&ltCastVariable2&gt&gt</code>) */
	protected Map fObsoleteCasts= null;

	/** The working copy owner */
	protected final WorkingCopyOwner fOwner= new WorkingCopyOwner() {
	};

	/** The type occurrences (element type: <code>&ltICompilationUnit, Collection&ltIDeclaredConstraintVariable&gt&gt</code>) */
	protected Map fTypeOccurrences= null;

	/**
	 * Creates the super type constraint solver to solve the model.
	 * 
	 * @param model the model to create a solver for
	 * @return The created super type constraint solver
	 */
	protected abstract SuperTypeConstraintsSolver createContraintSolver(SuperTypeConstraintsModel model);

	/**
	 * Returns the field which corresponds to the specified variable declaration fragment
	 * 
	 * @param fragment the variable declaration fragment
	 * @return the corresponding field
	 * @throws JavaModelException if an error occurs
	 */
	protected final IField getCorrespondingField(final VariableDeclarationFragment fragment) throws JavaModelException {
		final IBinding binding= fragment.getName().resolveBinding();
		if (binding instanceof IVariableBinding) {
			final IVariableBinding variable= (IVariableBinding) binding;
			if (variable.isField()) {
				final ICompilationUnit unit= RefactoringASTParser.getCompilationUnit(fragment);
				final IJavaElement element= unit.getElementAt(fragment.getStartPosition());
				if (element instanceof IField)
					return (IField) element;
			}
		}
		return null;
	}

	/**
	 * Computes the compilation units of fields referencing the specified type occurrences.
	 * 
	 * @param units the compilation unit map (element type: <code>&ltIJavaProject, Set&ltICompilationUnit&gt&gt</code>)
	 * @param nodes the ast nodes representing the type occurrences
	 * @throws JavaModelException if an error occurs
	 */
	protected final void getFieldReferencingCompilationUnits(final Map units, final ASTNode[] nodes) throws JavaModelException {
		ASTNode node= null;
		IField field= null;
		IJavaProject project= null;
		for (int index= 0; index < nodes.length; index++) {
			node= nodes[index];
			project= RefactoringASTParser.getCompilationUnit(node).getJavaProject();
			if (project != null) {
				final List fields= getReferencingFields(node, project);
				for (int offset= 0; offset < fields.size(); offset++) {
					field= (IField) fields.get(offset);
					Set set= (Set) units.get(project);
					if (set == null) {
						set= new HashSet();
						units.put(project, set);
					}
					final ICompilationUnit unit= field.getCompilationUnit();
					if (unit != null)
						set.add(unit);
				}
			}
		}
	}

	/**
	 * Computes the compilation units of methods referencing the specified type occurrences.
	 * 
	 * @param units the compilation unit map (element type: <code>&ltIJavaProject, Set&ltICompilationUnit&gt&gt</code>)
	 * @param nodes the ast nodes representing the type occurrences
	 * @throws JavaModelException if an error occurs
	 */
	protected final void getMethodReferencingCompilationUnits(final Map units, final ASTNode[] nodes) throws JavaModelException {
		ASTNode node= null;
		IMethod method= null;
		IJavaProject project= null;
		for (int index= 0; index < nodes.length; index++) {
			node= nodes[index];
			project= RefactoringASTParser.getCompilationUnit(node).getJavaProject();
			if (project != null) {
				method= getReferencingMethod(node);
				if (method != null) {
					Set set= (Set) units.get(project);
					if (set == null) {
						set= new HashSet();
						units.put(project, set);
					}
					final ICompilationUnit unit= method.getCompilationUnit();
					if (unit != null)
						set.add(unit);
				}
			}
		}
	}

	/**
	 * Computes the compilation units referencing the subtype to replace.
	 * 
	 * @param type the subtype
	 * @param monitor the progress monitor to use
	 * @param status the refactoring status
	 * @return the referenced compilation units (element type: <code>&ltIJavaProject, Collection&ltSearchResultGroup&gt&gt</code>)
	 * @throws JavaModelException if an error occurs
	 */
	protected final Map getReferencingCompilationUnits(final IType type, final IProgressMonitor monitor, final RefactoringStatus status) throws JavaModelException {
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.SuperTypeRefactoringProcessor_creating);
			final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2();
			engine.setOwner(fOwner);
			engine.setFiltering(true, true);
			engine.setStatus(status);
			engine.setScope(RefactoringScopeFactory.create(type));
			engine.setPattern(SearchPattern.createPattern(type, IJavaSearchConstants.REFERENCES, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE));
			engine.searchPattern(new SubProgressMonitor(monitor, 1));
			return engine.getAffectedProjects();
		} finally {
			monitor.done();
		}
	}

	/**
	 * Returns the fields which reference the specified ast node.
	 * 
	 * @param node the ast node
	 * @param project the java project
	 * @return the referencing fields
	 * @throws JavaModelException if an error occurs
	 */
	protected final List getReferencingFields(final ASTNode node, final IJavaProject project) throws JavaModelException {
		List result= Collections.EMPTY_LIST;
		if (node instanceof Type) {
			final BodyDeclaration parent= (BodyDeclaration) ASTNodes.getParent(node, BodyDeclaration.class);
			if (parent instanceof FieldDeclaration) {
				final List fragments= ((FieldDeclaration) parent).fragments();
				result= new ArrayList(fragments.size());
				VariableDeclarationFragment fragment= null;
				for (final Iterator iterator= fragments.iterator(); iterator.hasNext();) {
					fragment= (VariableDeclarationFragment) iterator.next();
					final IField field= getCorrespondingField(fragment);
					if (field != null)
						result.add(field);
				}
			}
		}
		return result;
	}

	/**
	 * Returns the method which references the specified ast node.
	 * 
	 * @param node the ast node
	 * @return the referencing method
	 * @throws JavaModelException if an error occurs
	 */
	protected final IMethod getReferencingMethod(final ASTNode node) throws JavaModelException {
		if (node instanceof Type) {
			final BodyDeclaration parent= (BodyDeclaration) ASTNodes.getParent(node, BodyDeclaration.class);
			if (parent instanceof MethodDeclaration) {
				final IMethodBinding binding= ((MethodDeclaration) parent).resolveBinding();
				if (binding != null) {
					final ICompilationUnit unit= RefactoringASTParser.getCompilationUnit(node);
					final IJavaElement element= unit.getElementAt(node.getStartPosition());
					if (element instanceof IMethod)
						return (IMethod) element;
				}
			}
		}
		return null;
	}

	/**
	 * Returns whether type occurrences in instanceof's should be rewritten.
	 * 
	 * @return <code>true</code> if they are rewritten, <code>false</code> otherwise
	 */
	public final boolean isInstanceOf() {
		return fInstanceOf;
	}

	/**
	 * Performs the first pass of processing the affected compilation units.
	 * 
	 * @param creator the constraints creator to use
	 * @param units the compilation unit map (element type: <code>&ltIJavaProject, Set&ltICompilationUnit&gt&gt</code>)
	 * @param groups the search result group map (element type: <code>&ltICompilationUnit, SearchResultGroup&gt</code>)
	 * @param unit the compilation unit of the subtype
	 * @param node the compilation unit node of the subtype
	 */
	protected final void performFirstPass(final SuperTypeConstraintsCreator creator, final Map units, final Map groups, final ICompilationUnit unit, final CompilationUnit node) {
		node.setProperty(RefactoringASTParser.SOURCE_PROPERTY, unit);
		node.accept(creator);
		final SearchResultGroup group= (SearchResultGroup) groups.get(unit);
		if (group != null) {
			final ASTNode[] nodes= ASTNodeSearchUtil.getAstNodes(group.getSearchResults(), node);
			try {
				getMethodReferencingCompilationUnits(units, nodes);
				getFieldReferencingCompilationUnits(units, nodes);
			} catch (JavaModelException exception) {
				JavaPlugin.log(exception);
			}
		}
	}

	/**
	 * Performs the second pass of processing the affected compilation units.
	 * 
	 * @param creator the constraints creator to use
	 * @param unit the compilation unit of the subtype
	 * @param node the compilation unit node of the subtype
	 */
	protected final void performSecondPass(final SuperTypeConstraintsCreator creator, final ICompilationUnit unit, final CompilationUnit node) {
		node.setProperty(RefactoringASTParser.SOURCE_PROPERTY, unit);
		node.accept(creator);
	}

	/**
	 * Creates the necessary text edits to replace the subtype occurrence by a supertype.
	 * 
	 * @param range the compilation unit range
	 * @param estimate the type estimate
	 * @param requestor the ast requestor to use
	 * @param rewrite the compilation unit rewrite to use
	 * @param copy the compilation unit node of the working copy ast
	 * @param replacements the set of variable binding keys of formal parameters which must be replaced
	 * @param group the text edit group to use
	 */
	protected final void rewriteTypeOccurrence(final CompilationUnitRange range, final TType estimate, final ASTRequestor requestor, final CompilationUnitRewrite rewrite, final CompilationUnit copy, final Set replacements, final TextEditGroup group) {
		ASTNode node= null;
		IBinding binding= null;
		final CompilationUnit target= rewrite.getRoot();
		node= NodeFinder.perform(copy, range.getSourceRange());
		if (node != null) {
			node= ASTNodes.getNormalizedNode(node).getParent();
			if (node instanceof VariableDeclaration) {
				binding= ((VariableDeclaration) node).resolveBinding();
				node= target.findDeclaringNode(binding.getKey());
				if (node instanceof SingleVariableDeclaration) {
					rewriteTypeOccurrence(estimate, rewrite, ((SingleVariableDeclaration) node).getType(), group);
					if (node.getParent() instanceof MethodDeclaration) {
						binding= ((VariableDeclaration) node).resolveBinding();
						if (binding != null)
							replacements.add(binding.getKey());
					}
				}
			} else if (node instanceof VariableDeclarationStatement) {
				binding= ((VariableDeclaration) ((VariableDeclarationStatement) node).fragments().get(0)).resolveBinding();
				node= target.findDeclaringNode(binding.getKey());
				if (node instanceof VariableDeclarationFragment)
					rewriteTypeOccurrence(estimate, rewrite, ((VariableDeclarationStatement) ((VariableDeclarationFragment) node).getParent()).getType(), group);
			} else if (node instanceof MethodDeclaration) {
				binding= ((MethodDeclaration) node).resolveBinding();
				node= target.findDeclaringNode(binding.getKey());
				if (node instanceof MethodDeclaration)
					rewriteTypeOccurrence(estimate, rewrite, ((MethodDeclaration) node).getReturnType2(), group);
			} else if (node instanceof FieldDeclaration) {
				binding= ((VariableDeclaration) ((FieldDeclaration) node).fragments().get(0)).resolveBinding();
				node= target.findDeclaringNode(binding.getKey());
				if (node instanceof VariableDeclarationFragment) {
					node= node.getParent();
					if (node instanceof FieldDeclaration)
						rewriteTypeOccurrence(estimate, rewrite, ((FieldDeclaration) node).getType(), group);
				}
			} else if (node instanceof ArrayType) {
				final ASTNode type= node;
				while (node != null && !(node instanceof MethodDeclaration) && !(node instanceof VariableDeclarationFragment))
					node= node.getParent();
				if (node != null) {
					final int delta= node.getStartPosition() + node.getLength() - type.getStartPosition();
					if (node instanceof MethodDeclaration)
						binding= ((MethodDeclaration) node).resolveBinding();
					else if (node instanceof VariableDeclarationFragment)
						binding= ((VariableDeclarationFragment) node).resolveBinding();
					if (binding != null) {
						node= target.findDeclaringNode(binding.getKey());
						if (node instanceof MethodDeclaration || node instanceof VariableDeclarationFragment) {
							node= NodeFinder.perform(target, node.getStartPosition() + node.getLength() - delta, 0);
							if (node instanceof SimpleName)
								rewriteTypeOccurrence(estimate, rewrite, node, group);
						}
					}
				}
			} else if (node instanceof QualifiedName) {
				final ASTNode name= node;
				while (node != null && !(node instanceof MethodDeclaration) && !(node instanceof VariableDeclarationFragment))
					node= node.getParent();
				if (node != null) {
					final int delta= node.getStartPosition() + node.getLength() - name.getStartPosition();
					if (node instanceof MethodDeclaration)
						binding= ((MethodDeclaration) node).resolveBinding();
					else if (node instanceof VariableDeclarationFragment)
						binding= ((VariableDeclarationFragment) node).resolveBinding();
					if (binding != null) {
						node= target.findDeclaringNode(binding.getKey());
						if (node instanceof SimpleName || node instanceof MethodDeclaration || node instanceof VariableDeclarationFragment) {
							node= NodeFinder.perform(target, node.getStartPosition() + node.getLength() - delta, 0);
							if (node instanceof SimpleName)
								rewriteTypeOccurrence(estimate, rewrite, node, group);
						}
					}
				}
			} else if (node instanceof CastExpression) {
				final ASTNode expression= node;
				while (node != null && !(node instanceof MethodDeclaration))
					node= node.getParent();
				if (node != null) {
					final int delta= node.getStartPosition() + node.getLength() - expression.getStartPosition();
					binding= ((MethodDeclaration) node).resolveBinding();
					node= target.findDeclaringNode(binding.getKey());
					if (node instanceof MethodDeclaration) {
						node= NodeFinder.perform(target, node.getStartPosition() + node.getLength() - delta, 0);
						if (node instanceof CastExpression)
							rewriteTypeOccurrence(estimate, rewrite, ((CastExpression) node).getType(), group);
					}
				}
			}
		}
	}

	/**
	 * Creates the necessary text edits to replace the subtype occurrence by a supertype.
	 * 
	 * @param estimate the type estimate
	 * @param rewrite the ast rewrite to use
	 * @param node the ast node to rewrite
	 * @param group the text edit group to use
	 */
	protected final void rewriteTypeOccurrence(final TType estimate, final CompilationUnitRewrite rewrite, final ASTNode node, final TextEditGroup group) {
		rewrite.getImportRemover().registerRemovedNode(node);
		rewrite.getASTRewrite().replace(node, createCorrespondingNode(rewrite, estimate), group);
	}

	/**
	 * Creates the necessary text edits to replace the subtype occurrence by a supertype.
	 * 
	 * @param manager the text change manager to use
	 * @param requestor the ast requestor to use
	 * @param rewrite the compilation unit rewrite of the subtype (not in working copy mode)
	 * @param unit the compilation unit
	 * @param node the compilation unit node
	 * @param replacements the set of variable binding keys of formal parameters which must be replaced
	 * @throws CoreException if the change could not be generated
	 */
	protected abstract void rewriteTypeOccurrences(TextChangeManager manager, ASTRequestor requestor, CompilationUnitRewrite rewrite, ICompilationUnit unit, CompilationUnit node, final Set replacements) throws CoreException;

	/**
	 * Creates the necessary text edits to replace the subtype occurrences by a supertype.
	 * 
	 * @param manager the text change manager to use
	 * @param sourceRewrite the compilation unit rewrite of the subtype (not in working copy mode)
	 * @param sourceRequestor the ast requestor of the subtype, or <code>null</code>
	 * @param subUnit the compilation unit of the subtype, or <code>null</code>
	 * @param subNode the compilation unit node of the subtype, or <code>null</code>
	 * @param replacements the set of variable binding keys of formal parameters which must be replaced
	 * @param status the refactoring status
	 * @param monitor the progress monitor to use
	 */
	protected final void rewriteTypeOccurrences(final TextChangeManager manager, final ASTRequestor sourceRequestor, final CompilationUnitRewrite sourceRewrite, final ICompilationUnit subUnit, final CompilationUnit subNode, final Set replacements, final RefactoringStatus status, final IProgressMonitor monitor) {
		if (fTypeOccurrences != null) {
			final Set units= new HashSet(fTypeOccurrences.keySet());
			if (subUnit != null)
				units.remove(subUnit);
			final Map projects= new HashMap();
			Collection collection= null;
			IJavaProject project= null;
			ICompilationUnit current= null;
			for (final Iterator iterator= units.iterator(); iterator.hasNext();) {
				current= (ICompilationUnit) iterator.next();
				project= current.getJavaProject();
				collection= (Collection) projects.get(project);
				if (collection == null) {
					collection= new ArrayList();
					projects.put(project, collection);
				}
				collection.add(current);
			}
			final ASTParser parser= ASTParser.newParser(AST.JLS3);
			final IProgressMonitor subMonitor= new SubProgressMonitor(monitor, 1);
			try {
				final Set keySet= projects.keySet();
				subMonitor.beginTask("", keySet.size()); //$NON-NLS-1$
				subMonitor.setTaskName(RefactoringCoreMessages.SuperTypeRefactoringProcessor_creating);
				for (final Iterator iterator= keySet.iterator(); iterator.hasNext();) {
					project= (IJavaProject) iterator.next();
					collection= (Collection) projects.get(project);
					parser.setWorkingCopyOwner(fOwner);
					parser.setResolveBindings(true);
					parser.setProject(project);
					parser.setCompilerOptions(RefactoringASTParser.getCompilerOptions(project));
					final IProgressMonitor subsubMonitor= new SubProgressMonitor(subMonitor, 1);
					try {
						subsubMonitor.beginTask("", collection.size()); //$NON-NLS-1$
						subsubMonitor.setTaskName(RefactoringCoreMessages.SuperTypeRefactoringProcessor_creating);
						parser.createASTs((ICompilationUnit[]) collection.toArray(new ICompilationUnit[collection.size()]), new String[0], new ASTRequestor() {

							public final void acceptAST(final ICompilationUnit unit, final CompilationUnit node) {
								try {
									rewriteTypeOccurrences(manager, this, sourceRewrite, unit, node, replacements);
								} catch (CoreException exception) {
									status.merge(RefactoringStatus.createFatalErrorStatus(exception.getLocalizedMessage()));
								} finally {
									subsubMonitor.worked(1);
								}
							}

							public final void acceptBinding(final String key, final IBinding binding) {
								// Do nothing
							}
						}, subsubMonitor);
					} finally {
						subsubMonitor.done();
					}
				}
			} finally {
				subMonitor.done();
			}
			try {
				if (subUnit != null && subNode != null && sourceRewrite != null && sourceRequestor != null)
					rewriteTypeOccurrences(manager, sourceRequestor, sourceRewrite, subUnit, subNode, replacements);
			} catch (CoreException exception) {
				status.merge(RefactoringStatus.createFatalErrorStatus(exception.getLocalizedMessage()));
			}
		}
	}

	/**
	 * Determines whether type occurrences in instanceof's should be rewritten.
	 * 
	 * @param rewrite <code>true</code> to rewrite them, <code>false</code> otherwise
	 */
	public final void setInstanceOf(final boolean rewrite) {
		fInstanceOf= rewrite;
	}

	/**
	 * Solves the supertype constraints to replace subtype by a supertype.
	 * 
	 * @param subUnit the compilation unit of the subtype, or <code>null</code>
	 * @param subNode the compilation unit node of the subtype, or <code>null</code>
	 * @param subType the java element of the subtype
	 * @param subBinding the type binding of the subtype to replace
	 * @param superBinding the type binding of the supertype to use as replacement
	 * @param monitor the progress monitor to use
	 * @param status the refactoring status
	 * @throws JavaModelException if an error occurs
	 */
	protected final void solveSuperTypeConstraints(final ICompilationUnit subUnit, final CompilationUnit subNode, final IType subType, final ITypeBinding subBinding, final ITypeBinding superBinding, final IProgressMonitor monitor, final RefactoringStatus status) throws JavaModelException {
		Assert.isNotNull(subType);
		Assert.isNotNull(subBinding);
		Assert.isNotNull(superBinding);
		Assert.isNotNull(monitor);
		Assert.isNotNull(status);
		int level= 3;
		TypeEnvironment environment= new TypeEnvironment();
		final SuperTypeConstraintsModel model= new SuperTypeConstraintsModel(environment, environment.create(subBinding), environment.create(superBinding));
		final SuperTypeConstraintsCreator creator= new SuperTypeConstraintsCreator(model, fInstanceOf);
		try {
			monitor.beginTask("", 3); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.SuperTypeRefactoringProcessor_creating);
			final Map firstPass= getReferencingCompilationUnits(subType, new SubProgressMonitor(monitor, 1), status);
			final Map secondPass= new HashMap();
			IJavaProject project= null;
			Collection collection= null;
			try {
				final ASTParser parser= ASTParser.newParser(AST.JLS3);
				Object element= null;
				ICompilationUnit current= null;
				SearchResultGroup group= null;
				SearchMatch[] matches= null;
				final Map groups= new HashMap();
				for (final Iterator outer= firstPass.keySet().iterator(); outer.hasNext();) {
					project= (IJavaProject) outer.next();
					if (level == 3 && !JavaModelUtil.is50OrHigher(project))
						level= 2;
					collection= (Collection) firstPass.get(project);
					if (collection != null) {
						for (final Iterator inner= collection.iterator(); inner.hasNext();) {
							group= (SearchResultGroup) inner.next();
							matches= group.getSearchResults();
							for (int index= 0; index < matches.length; index++) {
								element= matches[index].getElement();
								if (element instanceof IMember) {
									current= ((IMember) element).getCompilationUnit();
									if (current != null)
										groups.put(current, group);
								}
							}
						}
					}
				}
				Set units= null;
				final Set processed= new HashSet();
				if (subUnit != null)
					processed.add(subUnit);
				model.beginCreation();
				IProgressMonitor subMonitor= new SubProgressMonitor(monitor, 1);
				try {
					final Set keySet= firstPass.keySet();
					subMonitor.beginTask("", keySet.size()); //$NON-NLS-1$
					subMonitor.setTaskName(RefactoringCoreMessages.SuperTypeRefactoringProcessor_creating);
					for (final Iterator outer= keySet.iterator(); outer.hasNext();) {
						project= (IJavaProject) outer.next();
						collection= (Collection) firstPass.get(project);
						if (collection != null) {
							units= new HashSet(collection.size());
							for (final Iterator inner= collection.iterator(); inner.hasNext();) {
								group= (SearchResultGroup) inner.next();
								matches= group.getSearchResults();
								for (int index= 0; index < matches.length; index++) {
									element= matches[index].getElement();
									if (element instanceof IMember) {
										current= ((IMember) element).getCompilationUnit();
										if (current != null)
											units.add(current);
									}
								}
							}
							final List batches= new ArrayList(units);
							final int size= batches.size();
							final int iterations= ((size - 1) / SIZE_BATCH) + 1;
							final IProgressMonitor subsubMonitor= new SubProgressMonitor(subMonitor, 1);
							subsubMonitor.beginTask("", iterations); //$NON-NLS-1$
							subsubMonitor.setTaskName(RefactoringCoreMessages.SuperTypeRefactoringProcessor_creating);
							final Map options= RefactoringASTParser.getCompilerOptions(project);
							for (int index= 0; index < iterations; index++) {
								final List iteration= batches.subList(index * SIZE_BATCH, Math.min(size, (index + 1) * SIZE_BATCH));
								parser.setWorkingCopyOwner(fOwner);
								parser.setResolveBindings(true);
								parser.setProject(project);
								parser.setCompilerOptions(options);
								final IProgressMonitor subsubsubMonitor= new SubProgressMonitor(subsubMonitor, 1);
								try {
									final int count= iteration.size();
									subsubsubMonitor.beginTask("", count); //$NON-NLS-1$
									subsubsubMonitor.setTaskName(RefactoringCoreMessages.SuperTypeRefactoringProcessor_creating);
									parser.createASTs((ICompilationUnit[]) iteration.toArray(new ICompilationUnit[count]), new String[0], new ASTRequestor() {

										public final void acceptAST(final ICompilationUnit unit, final CompilationUnit node) {
											try {
												subsubsubMonitor.subTask(unit.getElementName());
												if (!processed.contains(unit)) {
													performFirstPass(creator, secondPass, groups, unit, node);
													processed.add(unit);
												}
											} finally {
												subsubsubMonitor.worked(1);
											}
										}

										public final void acceptBinding(final String key, final IBinding binding) {
											// Do nothing
										}
									}, subsubsubMonitor);
								} finally {
									subsubsubMonitor.done();
								}
							}
						}
					}
				} finally {
					firstPass.clear();
					subMonitor.done();
				}
				if (subUnit != null && subNode != null)
					performFirstPass(creator, secondPass, groups, subUnit, subNode);
				subMonitor= new SubProgressMonitor(monitor, 1);
				try {
					final Set keySet= secondPass.keySet();
					subMonitor.beginTask("", keySet.size()); //$NON-NLS-1$
					subMonitor.setTaskName(RefactoringCoreMessages.SuperTypeRefactoringProcessor_creating);
					for (final Iterator iterator= keySet.iterator(); iterator.hasNext();) {
						project= (IJavaProject) iterator.next();
						if (level == 3 && !JavaModelUtil.is50OrHigher(project))
							level= 2;
						collection= (Collection) secondPass.get(project);
						if (collection != null) {
							parser.setWorkingCopyOwner(fOwner);
							parser.setResolveBindings(true);
							parser.setProject(project);
							parser.setCompilerOptions(RefactoringASTParser.getCompilerOptions(project));
							final IProgressMonitor subsubMonitor= new SubProgressMonitor(subMonitor, 1);
							try {
								subsubMonitor.beginTask("", collection.size()); //$NON-NLS-1$
								subsubMonitor.setTaskName(RefactoringCoreMessages.SuperTypeRefactoringProcessor_creating);
								parser.createASTs((ICompilationUnit[]) collection.toArray(new ICompilationUnit[collection.size()]), new String[0], new ASTRequestor() {

									public final void acceptAST(final ICompilationUnit unit, final CompilationUnit node) {
										try {
											subsubMonitor.subTask(unit.getElementName());
											if (!processed.contains(unit))
												performSecondPass(creator, unit, node);
										} finally {
											subsubMonitor.worked(1);
										}
									}

									public final void acceptBinding(final String key, final IBinding binding) {
										// Do nothing
									}
								}, subsubMonitor);
							} finally {
								subsubMonitor.done();
							}
						}
					}
				} finally {
					secondPass.clear();
					subMonitor.done();
				}
			} finally {
				model.endCreation();
				model.setCompliance(level);
			}
			final SuperTypeConstraintsSolver solver= createContraintSolver(model);
			solver.solveConstraints();
			fTypeOccurrences= solver.getTypeOccurrences();
			fObsoleteCasts= solver.getObsoleteCasts();
		} finally {
			monitor.done();
		}
	}
}