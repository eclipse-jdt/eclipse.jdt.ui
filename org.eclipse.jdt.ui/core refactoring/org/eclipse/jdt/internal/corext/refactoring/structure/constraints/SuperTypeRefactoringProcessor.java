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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
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
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine2;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.CompilationUnitRange;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.SearchUtils;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Partial implementation of a refactoring processor solving supertype constraint models.
 * 
 * @since 3.1
 */
public abstract class SuperTypeRefactoringProcessor extends RefactoringProcessor {

	/**
	 * Returns the field which corresponds to the specified variable declaration fragment
	 * 
	 * @param fragment the variable declaration fragment
	 * @param project the java project
	 * @return the corresponding field
	 * @throws JavaModelException if an error occurs
	 */
	protected static IField getCorrespondingField(final VariableDeclarationFragment fragment, final IJavaProject project) throws JavaModelException {
		Assert.isNotNull(fragment);
		Assert.isNotNull(project);
		final IBinding binding= fragment.getName().resolveBinding();
		if (binding instanceof IVariableBinding) {
			final IVariableBinding variable= (IVariableBinding) binding;
			if (variable.isField())
				return Bindings.findField(variable, project);
		}
		return null;
	}

	/**
	 * Returns the fields which reference the specified ast node.
	 * 
	 * @param node the ast node
	 * @param project the java project
	 * @return the referencing fields
	 * @throws JavaModelException if an error occurs
	 */
	protected static List getReferencingFields(final ASTNode node, final IJavaProject project) throws JavaModelException {
		Assert.isNotNull(node);
		Assert.isNotNull(project);
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
	protected static IMethod getReferencingMethod(final ASTNode node, final IJavaProject project) throws JavaModelException {
		Assert.isNotNull(node);
		Assert.isNotNull(project);
		if (node instanceof Type) {
			final BodyDeclaration parent= (BodyDeclaration) ASTNodes.getParent(node, BodyDeclaration.class);
			if (parent instanceof MethodDeclaration) {
				final IMethodBinding binding= ((MethodDeclaration) parent).resolveBinding();
				if (binding != null)
					return Bindings.findMethod(binding, project);
			}
		}
		return null;
	}

	/** The obsolete casts (element type: <code>&ltICompilationUnit, Collection&ltCastVariable2&gt&gt</code>) */
	protected Map fObsoleteCasts= null;

	/** The working copy owner */
	protected final WorkingCopyOwner fOwner= new WorkingCopyOwner() {
	};

	/** The type matches (element type: <code>&ltICompilationUnit, Collection&ltIDeclaredConstraintVariable&gt</code>) */
	protected Map fTypeMatches= null;

	/** The type name */
	protected String fTypeName;

	/**
	 * Computes the possible supertype changes to be made by the refactoring.
	 * 
	 * @param type the subtype java element
	 * @param subType the type binding of the subtype to replace
	 * @param superType the type binding of the supertype to use as replacement
	 * @param monitor the progress monitor to use
	 * @param status the refactoring status
	 * @throws JavaModelException if an error occurs
	 */
	protected final void computeSupertypeChanges(final IType type, final ITypeBinding subType, final ITypeBinding superType, final IProgressMonitor monitor, final RefactoringStatus status) throws JavaModelException {
		Assert.isNotNull(type);
		Assert.isNotNull(subType);
		Assert.isNotNull(superType);
		Assert.isNotNull(monitor);
		Assert.isNotNull(status);
		final SuperTypeConstraintsModel model= new SuperTypeConstraintsModel();
		final SuperTypeConstraintsCreator creator= new SuperTypeConstraintsCreator(model);
		try {
			monitor.beginTask("", 2); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("SuperTypeRefactoringProcessor.creating")); //$NON-NLS-1$
			final Map firstPass= getReferencingCompilationUnits(type, new SubProgressMonitor(monitor, 1), status);
			final Map secondPass= new HashMap(firstPass.size());
			IJavaProject project= null;
			Collection collection= null;
			for (final Iterator iterator= firstPass.keySet().iterator(); iterator.hasNext();) {
				project= (IJavaProject) iterator.next();
				collection= (Collection) firstPass.get(project);
				if (collection != null) {
					final Map groups= new HashMap(collection.size());
					for (final Iterator iter= collection.iterator(); iter.hasNext();) {
						final SearchResultGroup group= (SearchResultGroup) iter.next();
						groups.put(group.getCompilationUnit(), group);
					}
					final ASTParser parser= ASTParser.newParser(AST.JLS3);
					parser.setCompilerOptions(RefactoringASTParser.getCompilerOptions(project));
					parser.setResolveBindings(true);
					parser.setProject(project);
					final Set keys= groups.keySet();
					parser.createASTs((ICompilationUnit[]) keys.toArray(new ICompilationUnit[keys.size()]), new String[0], new ASTRequestor() {

						public final void acceptAST(final ICompilationUnit unit, final CompilationUnit node) {
							monitor.subTask(unit.getElementName());
							node.setProperty(RefactoringASTParser.SOURCE_PROPERTY, unit);
							node.accept(creator);
							final SearchResultGroup group= (SearchResultGroup) groups.get(unit);
							if (group != null) {
								final ASTNode[] nodes= ASTNodeSearchUtil.getAstNodes(group.getSearchResults(), node);
								try {
									getMethodReferencingCompilationUnits(secondPass, nodes);
									getFieldReferencingCompilationUnits(secondPass, nodes);
								} catch (JavaModelException exception) {
									JavaPlugin.log(exception);
								}
							}
						}

						public final void acceptBinding(final String key, final IBinding binding) {
							// Do nothing
						}
					}, new SubProgressMonitor(monitor, 1));
				}
			}
			for (final Iterator iterator= secondPass.keySet().iterator(); iterator.hasNext();) {
				project= (IJavaProject) iterator.next();
				collection= (Set) secondPass.get(project);
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
			fTypeMatches= solver.getTypeOccurrences();
			fObsoleteCasts= solver.getObsoleteCasts();
		} finally {
			monitor.done();
		}
	}

	/**
	 * Creates the necessary text edits to replace the subtype occurrence by a supertype.
	 * 
	 * @param range the compilation unit range
	 * @param rewrite the ast rewrite to use
	 * @param target the compilation unit node of the ast to rewrite
	 * @param source the compilation unit node of the working copy ast
	 * @param replacements the set of variable binding keys of formal parameters which must be replaced
	 * @param group the text edit group to use
	 */
	protected final void createTypeOccurrenceReplacement(final CompilationUnitRange range, final ASTRewrite rewrite, final CompilationUnit target, final CompilationUnit source, final Set replacements, final TextEditGroup group) {
		Assert.isNotNull(rewrite);
		Assert.isNotNull(target);
		Assert.isNotNull(source);
		Assert.isNotNull(range);
		Assert.isNotNull(replacements);
		ASTNode node= null;
		IBinding binding= null;
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
	 * Computes the compilation units of fields referencing the specified type occurrences.
	 * 
	 * @param map the compilation unit map (element type: <code>&ltIJavaProject, Set&ltICompilationUnit&gt&gt</code>)
	 * @param nodes the ast nodes representing the type occurrences
	 * @throws JavaModelException if an error occurs
	 */
	protected final void getFieldReferencingCompilationUnits(final Map map, final ASTNode[] nodes) throws JavaModelException {
		Assert.isNotNull(map);
		Assert.isNotNull(nodes);
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
					Set set= (Set) map.get(project);
					if (set == null) {
						set= new HashSet();
						map.put(project, set);
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
	 * @param map the compilation unit map (element type: <code>&ltIJavaProject, Set&ltICompilationUnit&gt&gt</code>)
	 * @param nodes the ast nodes representing the type occurrences
	 * @throws JavaModelException if an error occurs
	 */
	protected final void getMethodReferencingCompilationUnits(final Map map, final ASTNode[] nodes) throws JavaModelException {
		Assert.isNotNull(map);
		Assert.isNotNull(nodes);
		ASTNode node= null;
		IMethod method= null;
		IJavaProject project= null;
		for (int index= 0; index < nodes.length; index++) {
			node= nodes[index];
			project= RefactoringASTParser.getCompilationUnit(node).getJavaProject();
			if (project != null) {
				method= getReferencingMethod(node, project);
				if (method != null) {
					Set set= (Set) map.get(project);
					if (set == null) {
						set= new HashSet();
						map.put(project, set);
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
		Assert.isNotNull(type);
		Assert.isNotNull(monitor);
		Assert.isNotNull(status);
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
	 * Rewrites the specified ast node.
	 * 
	 * @param rewrite the ast rewrite to use
	 * @param target the target compilation unit node
	 * @param group the text edit group to use
	 * @param node the ast node to rewrite
	 */
	protected void rewriteArrayType(final ASTRewrite rewrite, final CompilationUnit target, final TextEditGroup group, final ASTNode node) {
		rewrite.replace(node, target.getAST().newSimpleName(fTypeName), group);
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
		rewrite.replace(node.getType(), target.getAST().newSimpleType(target.getAST().newSimpleName(fTypeName)), group);
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
		rewrite.replace(node.getType(), target.getAST().newSimpleType(target.getAST().newSimpleName(fTypeName)), group);
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
		rewrite.replace(node.getReturnType2(), target.getAST().newSimpleType(target.getAST().newSimpleName(fTypeName)), group);
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
		rewrite.replace(node, target.getAST().newSimpleName(fTypeName), group);
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
		rewrite.replace(node.getType(), target.getAST().newSimpleType(target.getAST().newSimpleName(fTypeName)), group);
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
		rewrite.replace(((VariableDeclarationStatement) node.getParent()).getType(), target.getAST().newSimpleType(target.getAST().newSimpleName(fTypeName)), group);
	}
}