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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
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
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
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
import org.eclipse.jdt.internal.corext.util.SearchUtils;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Partial implementation of a refactoring processor solving supertype constraint models.
 * 
 * @since 3.1
 */
public abstract class SuperTypeRefactoringProcessor extends RefactoringProcessor {

	/**
	 * Returns a new ast node corresponding to the given type.
	 * 
	 * @param ast the target ast
	 * @param type the specified type
	 * @return A corresponding ast node
	 */
	protected static ASTNode createCorrespondingNode(final AST ast, final TType type) {
		return ast.newSimpleType(ast.newSimpleName(type.getName()));
	}

	/** The type environment */
	protected final TypeEnvironment fEnvironment= new TypeEnvironment();

	/** Should type occurrences on instanceof's also be rewritten? */
	protected boolean fInstanceOf= false;

	/** The obsolete casts (element type: <code>&ltICompilationUnit, Collection&ltCastVariable2&gt&gt</code>) */
	protected Map fObsoleteCasts= null;

	/** The working copy owner */
	protected final WorkingCopyOwner fOwner= new WorkingCopyOwner() {
	};

	/** The super type as replacement */
	protected TType fSuperType= null;

	/** The type occurrences (element type: <code>&ltICompilationUnit, Collection&ltIDeclaredConstraintVariable&gt&gt</code>) */
	protected Map fTypeOccurrences= null;

	/**
	 * Returns the field which corresponds to the specified variable declaration fragment
	 * 
	 * @param fragment the variable declaration fragment
	 * @param project the java project
	 * @return the corresponding field
	 * @throws JavaModelException if an error occurs
	 */
	protected IField getCorrespondingField(final VariableDeclarationFragment fragment, final IJavaProject project) throws JavaModelException {
		final IBinding binding= fragment.getName().resolveBinding();
		if (binding instanceof IVariableBinding) {
			final IVariableBinding variable= (IVariableBinding) binding;
			if (variable.isField())
				return Bindings.findField(variable, project, fOwner);
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
				method= getReferencingMethod(node, project);
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
			monitor.setTaskName(RefactoringCoreMessages.getString("SuperTypeRefactoringProcessor.creating")); //$NON-NLS-1$
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
	protected List getReferencingFields(final ASTNode node, final IJavaProject project) throws JavaModelException {
		List result= Collections.EMPTY_LIST;
		if (node instanceof Type) {
			final BodyDeclaration parent= (BodyDeclaration) ASTNodes.getParent(node, BodyDeclaration.class);
			if (parent instanceof FieldDeclaration) {
				final List fragments= ((FieldDeclaration) parent).fragments();
				result= new ArrayList(fragments.size());
				VariableDeclarationFragment fragment= null;
				for (final Iterator iterator= fragments.iterator(); iterator.hasNext();) {
					fragment= (VariableDeclarationFragment) iterator.next();
					final IField field= getCorrespondingField(fragment, project);
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
	 * @param project the java project
	 * @return the referencing method
	 * @throws JavaModelException if an error occurs
	 */
	protected IMethod getReferencingMethod(final ASTNode node, final IJavaProject project) throws JavaModelException {
		if (node instanceof Type) {
			final BodyDeclaration parent= (BodyDeclaration) ASTNodes.getParent(node, BodyDeclaration.class);
			if (parent instanceof MethodDeclaration) {
				final IMethodBinding binding= ((MethodDeclaration) parent).resolveBinding();
				if (binding != null)
					return Bindings.findMethod(binding, project, fOwner);
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
	 * @param groups the search result group map
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
	 * Rewrites the specified ast node.
	 * 
	 * @param rewrite the ast rewrite to use
	 * @param target the target compilation unit node
	 * @param group the text edit group to use
	 * @param node the ast node to rewrite
	 */
	protected void rewriteArrayType(final ASTRewrite rewrite, final CompilationUnit target, final TextEditGroup group, final ASTNode node) {
		rewrite.replace(node, createCorrespondingNode(target.getAST(), fSuperType), group);
	}

	/**
	 * Rewrites the specified ast node.
	 * 
	 * @param rewrite the ast rewrite to use
	 * @param target the target compilation unit node
	 * @param group the text edit group to use
	 * @param node the ast node to rewrite
	 */
	protected void rewriteCastExpression(final ASTRewrite rewrite, final CompilationUnit target, final TextEditGroup group, final CastExpression node) {
		rewrite.replace(node.getType(), createCorrespondingNode(target.getAST(), fSuperType), group);
	}

	/**
	 * Rewrites the specified ast node.
	 * 
	 * @param rewrite the ast rewrite to use
	 * @param target the target compilation unit node
	 * @param group the text edit group to use
	 * @param node the ast node to rewrite
	 */
	protected void rewriteFieldDeclaration(final ASTRewrite rewrite, final CompilationUnit target, final TextEditGroup group, final FieldDeclaration node) {
		rewrite.replace(node.getType(), createCorrespondingNode(target.getAST(), fSuperType), group);
	}

	/**
	 * Rewrites the specified ast node.
	 * 
	 * @param rewrite the ast rewrite to use
	 * @param target the target compilation unit node
	 * @param group the text edit group to use
	 * @param node the ast node to rewrite
	 */
	protected void rewriteMethodReturnType(final ASTRewrite rewrite, final CompilationUnit target, final TextEditGroup group, final MethodDeclaration node) {
		rewrite.replace(node.getReturnType2(), createCorrespondingNode(target.getAST(), fSuperType), group);
	}

	/**
	 * Rewrites the specified ast node.
	 * 
	 * @param rewrite the ast rewrite to use
	 * @param target the target compilation unit node
	 * @param group the text edit group to use
	 * @param node the ast node to rewrite
	 */
	protected void rewriteQualifiedName(final ASTRewrite rewrite, final CompilationUnit target, final TextEditGroup group, final ASTNode node) {
		rewrite.replace(node, createCorrespondingNode(target.getAST(), fSuperType), group);
	}

	/**
	 * Rewrites the specified ast node.
	 * 
	 * @param rewrite the ast rewrite to use
	 * @param target the target compilation unit node
	 * @param group the text edit group to use
	 * @param node the ast node to rewrite
	 */
	protected void rewriteSingleVariableDeclaration(final ASTRewrite rewrite, final CompilationUnit target, final TextEditGroup group, final SingleVariableDeclaration node) {
		rewrite.replace(node.getType(), createCorrespondingNode(target.getAST(), fSuperType), group);
	}

	/**
	 * Creates the necessary text edits to replace the subtype occurrence by a supertype.
	 * 
	 * @param range the compilation unit range
	 * @param rewriter the compilation unit rewrite to use
	 * @param source the compilation unit node of the working copy ast
	 * @param replacements the set of variable binding keys of formal parameters which must be replaced
	 * @param group the text edit group to use
	 */
	protected final void rewriteTypeOccurrence(final CompilationUnitRange range, final CompilationUnitRewrite rewriter, final CompilationUnit source, final Set replacements, final TextEditGroup group) {
		ASTNode node= null;
		IBinding binding= null;
		final CompilationUnit target= rewriter.getRoot();
		final ASTRewrite rewrite= rewriter.getASTRewrite();
		node= NodeFinder.perform(source, range.getSourceRange());
		if (node != null) {
			node= ASTNodes.getNormalizedNode(node).getParent();
			if (node instanceof VariableDeclaration) {
				binding= ((VariableDeclaration) node).resolveBinding();
				node= target.findDeclaringNode(binding.getKey());
				if (node instanceof SingleVariableDeclaration) {
					rewriteSingleVariableDeclaration(rewrite, target, group, (SingleVariableDeclaration) node);
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
					rewriteVariableDeclarationFragment(rewrite, target, group, (VariableDeclarationFragment) node);
			} else if (node instanceof MethodDeclaration) {
				binding= ((MethodDeclaration) node).resolveBinding();
				node= target.findDeclaringNode(binding.getKey());
				if (node instanceof MethodDeclaration)
					rewriteMethodReturnType(rewrite, target, group, (MethodDeclaration) node);
			} else if (node instanceof FieldDeclaration) {
				binding= ((VariableDeclaration) ((FieldDeclaration) node).fragments().get(0)).resolveBinding();
				node= target.findDeclaringNode(binding.getKey());
				if (node instanceof VariableDeclarationFragment) {
					node= node.getParent();
					if (node instanceof FieldDeclaration)
						rewriteFieldDeclaration(rewrite, target, group, (FieldDeclaration) node);
				}
			} else if (node instanceof ArrayType) {
				final ASTNode type= node;
				while (node != null && !(node instanceof MethodDeclaration) && !(node instanceof VariableDeclarationFragment))
					node= node.getParent();
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
							rewriteArrayType(rewrite, target, group, node);
					}
				}
			} else if (node instanceof QualifiedName) {
				final ASTNode name= node;
				while (node != null && !(node instanceof MethodDeclaration) && !(node instanceof VariableDeclarationFragment))
					node= node.getParent();
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
							rewriteQualifiedName(rewrite, target, group, node);
					}
				}
			} else if (node instanceof CastExpression) {
				final ASTNode expression= node;
				while (node != null && !(node instanceof MethodDeclaration))
					node= node.getParent();
				final int delta= node.getStartPosition() + node.getLength() - expression.getStartPosition();
				binding= ((MethodDeclaration) node).resolveBinding();
				node= target.findDeclaringNode(binding.getKey());
				if (node instanceof MethodDeclaration) {
					node= NodeFinder.perform(target, node.getStartPosition() + node.getLength() - delta, 0);
					if (node instanceof CastExpression)
						rewriteCastExpression(rewrite, target, group, (CastExpression) node);
				}
			}
		}
	}

	/**
	 * Creates the necessary text edits to replace the subtype occurrence by a supertype.
	 * 
	 * @param range the compilation unit range
	 * @param rewriter the compilation unit rewrite to use
	 * @param target the compilation unit node of the ast to rewrite
	 * @param group the text edit group to use
	 */
	protected final void rewriteTypeOccurrence(final CompilationUnitRange range, final CompilationUnitRewrite rewriter, final CompilationUnit target, final TextEditGroup group) {
		final ASTNode node= NodeFinder.perform(target, range.getSourceRange());
		if (node != null)
			rewriteTypeOccurrence(rewriter, node, group);
	}

	/**
	 * Creates the necessary text edits to replace the subtype occurrence by a supertype.
	 * 
	 * @param rewrite the ast rewrite to use
	 * @param node the ast node to rewrite
	 * @param group the text edit group to use
	 */
	protected final void rewriteTypeOccurrence(final CompilationUnitRewrite rewrite, final ASTNode node, final TextEditGroup group) {
		rewrite.getASTRewrite().replace(node, createCorrespondingNode(rewrite.getAST(), fSuperType), group);
	}

	/**
	 * Creates the necessary text edits to replace the subtype occurrence by a supertype.
	 * 
	 * @param manager the text change manager to use
	 * @param subRewrite the compilation unit rewrite of the subtype
	 * @param unit the compilation unit
	 * @param node the compilation unit node
	 * @param replacements the set of variable binding keys of formal parameters which must be replaced
	 * @throws CoreException if the change could not be generated
	 */
	protected abstract void rewriteTypeOccurrences(TextChangeManager manager, CompilationUnitRewrite subRewrite, ICompilationUnit unit, CompilationUnit node, final Set replacements) throws CoreException;

	/**
	 * Creates the necessary text edits to replace the subtype occurrences by a supertype.
	 * 
	 * @param manager the text change manager to use
	 * @param subRewrite the compilation unit rewrite of the subtype
	 * @param subUnit the compilation unit of the subtype
	 * @param subNode the compilation unit node of the subtype
	 * @param replacements the set of variable binding keys of formal parameters which must be replaced
	 * @param status the refactoring status
	 * @param monitor the progress monitor to use
	 */
	protected final void rewriteTypeOccurrences(final TextChangeManager manager, final CompilationUnitRewrite subRewrite, final ICompilationUnit subUnit, final CompilationUnit subNode, final Set replacements, final RefactoringStatus status, final IProgressMonitor monitor) {
		if (fTypeOccurrences != null) {
			final Set units= new HashSet(fTypeOccurrences.keySet());
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
			for (final Iterator iterator= projects.keySet().iterator(); iterator.hasNext();) {
				project= (IJavaProject) iterator.next();
				collection= (Collection) projects.get(project);
				parser.setWorkingCopyOwner(fOwner);
				parser.setResolveBindings(true);
				parser.setCompilerOptions(RefactoringASTParser.getCompilerOptions(project));
				parser.setProject(project);
				parser.createASTs((ICompilationUnit[]) collection.toArray(new ICompilationUnit[collection.size()]), new String[0], new ASTRequestor() {

					public final void acceptAST(final ICompilationUnit unit, final CompilationUnit node) {
						try {
							rewriteTypeOccurrences(manager, subRewrite, unit, node, replacements);
						} catch (CoreException exception) {
							status.merge(RefactoringStatus.createErrorStatus(exception.getLocalizedMessage()));
						}
					}

					public final void acceptBinding(final String key, final IBinding binding) {
						// Do nothing
					}
				}, new SubProgressMonitor(monitor, 1));
			}
			try {
				rewriteTypeOccurrences(manager, subRewrite, subUnit, subNode, replacements);
			} catch (CoreException exception) {
				status.merge(RefactoringStatus.createErrorStatus(exception.getLocalizedMessage()));
			}
		}
	}

	/**
	 * Rewrites the specified ast node.
	 * 
	 * @param rewrite the ast rewrite to use
	 * @param target the target compilation unit node
	 * @param group the text edit group to use
	 * @param node the ast node to rewrite
	 */
	protected void rewriteVariableDeclarationFragment(final ASTRewrite rewrite, final CompilationUnit target, final TextEditGroup group, final VariableDeclarationFragment node) {
		rewrite.replace(((VariableDeclarationStatement) node.getParent()).getType(), createCorrespondingNode(target.getAST(), fSuperType), group);
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
	 * @param subUnit the compilation unit of the subtype
	 * @param subNode the compilation unit node of the subtype
	 * @param type the java element of the subtype
	 * @param subType the type binding of the subtype to replace
	 * @param superType the type binding of the supertype to use as replacement
	 * @param monitor the progress monitor to use
	 * @param status the refactoring status
	 * @throws JavaModelException if an error occurs
	 */
	protected final void solveSuperTypeConstraints(final ICompilationUnit subUnit, final CompilationUnit subNode, final IType type, final ITypeBinding subType, final ITypeBinding superType, final IProgressMonitor monitor, final RefactoringStatus status) throws JavaModelException {
		int level= AST.JLS3;
		final SuperTypeConstraintsModel model= new SuperTypeConstraintsModel(fEnvironment, fEnvironment.create(subType), fEnvironment.create(superType));
		final SuperTypeConstraintsCreator creator= new SuperTypeConstraintsCreator(model, fInstanceOf);
		fSuperType= model.getSuperType();
		try {
			monitor.beginTask("", 3); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("SuperTypeRefactoringProcessor.creating")); //$NON-NLS-1$
			final Map firstPass= getReferencingCompilationUnits(type, new SubProgressMonitor(monitor, 1), status);
			final Map secondPass= new HashMap(firstPass.size());
			IJavaProject project= null;
			Collection collection= null;
			try {
				final ASTParser parser= ASTParser.newParser(AST.JLS3);
				SearchResultGroup group= null;
				final Map groups= new HashMap();
				for (final Iterator outer= firstPass.keySet().iterator(); outer.hasNext();) {
					project= (IJavaProject) outer.next();
					if (level == AST.JLS3 && !JavaCore.VERSION_1_5.equals(project.getOption(JavaCore.COMPILER_COMPLIANCE, true)))
						level= AST.JLS2;
					collection= (Collection) firstPass.get(project);
					if (collection != null) {
						for (final Iterator inner= collection.iterator(); inner.hasNext();) {
							group= (SearchResultGroup) inner.next();
							groups.put(group.getCompilationUnit(), group);
						}
					}
				}
				for (final Iterator iterator= firstPass.keySet().iterator(); iterator.hasNext();) {
					project= (IJavaProject) iterator.next();
					collection= (Collection) firstPass.get(project);
					if (collection != null && !collection.isEmpty()) {
						parser.setWorkingCopyOwner(fOwner);
						parser.setResolveBindings(true);
						parser.setCompilerOptions(RefactoringASTParser.getCompilerOptions(project));
						parser.setProject(project);
						final Set units= new HashSet(groups.keySet());
						units.remove(subUnit);
						parser.createASTs((ICompilationUnit[]) units.toArray(new ICompilationUnit[units.size()]), new String[0], new ASTRequestor() {

							public final void acceptAST(final ICompilationUnit unit, final CompilationUnit node) {
								monitor.subTask(unit.getElementName());
								performFirstPass(creator, secondPass, groups, unit, node);
							}

							public final void acceptBinding(final String key, final IBinding binding) {
								// Do nothing
							}
						}, new SubProgressMonitor(monitor, 1));
					}
				}
				performFirstPass(creator, secondPass, groups, subUnit, subNode);
				for (final Iterator iterator= secondPass.keySet().iterator(); iterator.hasNext();) {
					project= (IJavaProject) iterator.next();
					if (level == AST.JLS3 && !JavaCore.VERSION_1_5.equals(project.getOption(JavaCore.COMPILER_COMPLIANCE, true)))
						level= AST.JLS2;
					collection= (Collection) secondPass.get(project);
					if (collection != null && !collection.isEmpty()) {
						parser.setWorkingCopyOwner(fOwner);
						parser.setResolveBindings(true);
						parser.setCompilerOptions(RefactoringASTParser.getCompilerOptions(project));
						parser.setProject(project);
						final Set units= new HashSet(collection);
						units.remove(subUnit);
						parser.createASTs((ICompilationUnit[]) units.toArray(new ICompilationUnit[units.size()]), new String[0], new ASTRequestor() {

							public final void acceptAST(final ICompilationUnit unit, final CompilationUnit node) {
								monitor.subTask(unit.getElementName());
								performSecondPass(creator, unit, node);
							}

							public final void acceptBinding(final String key, final IBinding binding) {
								// Do nothing
							}
						}, new SubProgressMonitor(monitor, 1));
					}
				}
			} finally {
				model.setCompliance(level);
			}
			final SuperTypeConstraintsSolver solver= new SuperTypeConstraintsSolver(model);
			solver.solveConstraints();
			fTypeOccurrences= solver.getTypeOccurrences();
			fObsoleteCasts= solver.getObsoleteCasts();
		} finally {
			monitor.done();
		}
	}
}