/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;

/**
 * Correction proposal to convert for loops over iterables to enhanced for loops.
 * 
 * @since 3.1
 */
public final class ConvertIterableLoopProposal extends LinkedCorrectionProposal {

	/** The linked position group id */
	private static final String GROUP_ID= "element"; //$NON-NLS-1$

	/**
	 * Returns the supertype of the given type with the qualified name.
	 * 
	 * @param binding the binding of the type
	 * @param name the qualified name of the supertype
	 * @return the supertype, or <code>null</code>
	 */
	private static ITypeBinding getSuperType(final ITypeBinding binding, final String name) {

		if (binding.isArray() || binding.isPrimitive())
			return null;

		if (binding.getQualifiedName().startsWith(name))
			return binding;

		final ITypeBinding type= binding.getSuperclass();
		if (type != null) {
			final ITypeBinding result= getSuperType(type, name);
			if (result != null)
				return result;
		}
		final ITypeBinding[] types= binding.getInterfaces();
		for (int index= 0; index < types.length; index++) {
			final ITypeBinding result= getSuperType(types[index], name);
			if (result != null)
				return result;
		}
		return null;
	}

	/** Has the element variable been assigned outside the for statement? */
	private boolean fAssigned= false;

	/** The ast to operate on */
	private final AST fAst;

	/** The binding of the element variable */
	private IBinding fElement= null;

	/** The node of the iterable object used in the expression */
	private Expression fExpression= null;

	/** The binding of the iterable object */
	private IBinding fIterable= null;

	/** The binding of the iterator variable */
	private IVariableBinding fIterator= null;

	/** The nodes of the element variable occurrences */
	private List fOccurrences= new ArrayList(2);

	/** The for statement to convert */
	private final ForStatement fStatement;

	/**
	 * Creates a new convert iterable loop proposal.
	 * 
	 * @param name the name of the correction proposal
	 * @param unit the compilation unit containing the for statement
	 * @param statement the for statement to be converted
	 * @param relevance the relevance of the proposal
	 * @param image the image of the proposal
	 */
	public ConvertIterableLoopProposal(final String name, final ICompilationUnit unit, final ForStatement statement, final int relevance, final Image image) {
		super(name, unit, null, relevance, image);
		fStatement= statement;
		fAst= statement.getAST();
	}

	/**
	 * Returns the expression for the enhanced for statement.
	 * 
	 * @param rewrite the ast rewrite to use
	 * @return the expression node
	 */
	private Expression getExpression(final ASTRewrite rewrite) {
		if (fExpression instanceof MethodInvocation)
			return (MethodInvocation) rewrite.createMoveTarget(fExpression);
		return (Expression) ASTNode.copySubtree(rewrite.getAST(), fExpression);
	}

	/**
	 * Returns the iterable type from the iterator type binding.
	 * 
	 * @param iterator the iterator type binding, or <code>null</code>
	 * @return the iterable type
	 */
	private ITypeBinding getIterableType(final ITypeBinding iterator) {
		if (iterator != null) {
			final ITypeBinding[] bindings= iterator.getTypeArguments();
			if (bindings.length > 0)
				return bindings[0];
		}
		return fAst.resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.correction.ASTRewriteCorrectionProposal#getRewrite()
	 */
	protected final ASTRewrite getRewrite() throws CoreException {
		final ASTRewrite rewrite= ASTRewrite.create(fAst);
		final EnhancedForStatement statement= fAst.newEnhancedForStatement();
		final Statement body= fStatement.getBody();
		if (body != null) {
			if (body instanceof Block) {
				final ListRewrite rewriter= rewrite.getListRewrite(body, Block.STATEMENTS_PROPERTY);
				for (final Iterator iterator= fOccurrences.iterator(); iterator.hasNext();) {
					final Statement parent= (Statement) ASTNodes.getParent((ASTNode) iterator.next(), Statement.class);
					if (parent != null && rewriter.getRewrittenList().contains(parent))
						rewriter.remove(parent, null);
				}
				body.accept(new ASTVisitor() {

					public final boolean visit(final SimpleName node) {
						final IBinding binding= node.resolveBinding();
						if (binding != null && binding.equals(fElement)) {
							final Statement parent= (Statement) ASTNodes.getParent(node, Statement.class);
							if (parent != null && rewriter.getRewrittenList().contains(parent))
								addLinkedPosition(rewrite.track(node), false, GROUP_ID);
						}
						return false;
					}
				});
			}
			statement.setBody((Statement) rewrite.createMoveTarget(body));
		}
		final SingleVariableDeclaration declaration= fAst.newSingleVariableDeclaration();
		final SimpleName name= fAst.newSimpleName(fElement.getName());
		addLinkedPosition(rewrite.track(name), true, GROUP_ID);
		declaration.setName(name);
		declaration.setType(getImportRewrite().addImport(getIterableType(fIterator.getType()), fAst));
		statement.setParameter(declaration);
		statement.setExpression(getExpression(rewrite));
		rewrite.replace(fStatement, statement, null);
		return rewrite;
	}

	/**
	 * Is this proposal applicable?
	 * 
	 * @return <code>true</code> if it is applicable, <code>false</code> otherwise
	 */
	public final boolean isApplicable() {
		for (final Iterator outer= fStatement.initializers().iterator(); outer.hasNext();) {
			final Expression expression= (Expression) outer.next();
			if (expression instanceof VariableDeclarationExpression) {
				final VariableDeclarationExpression declaration= (VariableDeclarationExpression) expression;
				for (Iterator inner= declaration.fragments().iterator(); inner.hasNext();) {
					final VariableDeclarationFragment fragment= (VariableDeclarationFragment) inner.next();
					fragment.accept(new ASTVisitor() {

						public final boolean visit(final MethodInvocation node) {
							final IMethodBinding binding= node.resolveMethodBinding();
							if (binding != null && binding.getName().equals("iterator")) { //$NON-NLS-1$
								final Expression qualifier= node.getExpression();
								if (qualifier != null) {
									final ITypeBinding type= qualifier.resolveTypeBinding();
									if (type != null) {
										final ITypeBinding iterable= getSuperType(type, "java.lang.Iterable"); //$NON-NLS-1$
										if (iterable != null) {
											fExpression= qualifier;
											if (qualifier instanceof Name) {
												final Name name= (Name) qualifier;
												fIterable= name.resolveBinding();
											} else if (qualifier instanceof MethodInvocation) {
												final MethodInvocation invocation= (MethodInvocation) qualifier;
												fIterable= invocation.resolveMethodBinding();
											} else if (qualifier instanceof FieldAccess) {
												final FieldAccess access= (FieldAccess) qualifier;
												fIterable= access.resolveFieldBinding();
											}
										}
									}
								}
							}
							return true;
						}

						public final boolean visit(final VariableDeclarationFragment node) {
							final IVariableBinding binding= node.resolveBinding();
							if (binding != null) {
								final ITypeBinding type= binding.getType();
								if (type != null) {
									final ITypeBinding iterator= getSuperType(type, "java.util.Iterator"); //$NON-NLS-1$
									if (iterator != null)
										fIterator= binding;
								}
							}
							return true;
						}
					});
				}
			}
		}
		final Statement statement= fStatement.getBody();
		if (statement != null && fIterator != null) {
			final ITypeBinding iterable= getIterableType(fIterator.getType());
			statement.accept(new ASTVisitor() {

				public final boolean visit(final Assignment node) {
					return visit(node.getLeftHandSide(), node.getRightHandSide());
				}

				private boolean visit(final Expression node) {
					if (node != null) {
						final ITypeBinding binding= node.resolveTypeBinding();
						if (binding != null && iterable.equals(binding)) {
							if (node instanceof Name) {
								final Name name= (Name) node;
								final IBinding result= name.resolveBinding();
								if (result != null) {
									fOccurrences.add(node);
									fElement= result;
									return false;
								}
							} else if (node instanceof FieldAccess) {
								final FieldAccess access= (FieldAccess) node;
								final IBinding result= access.resolveFieldBinding();
								if (result != null) {
									fOccurrences.add(node);
									fElement= result;
									return false;
								}
							}
						}
					}
					return true;
				}

				private boolean visit(final Expression left, final Expression right) {
					if (right instanceof MethodInvocation) {
						final MethodInvocation invocation= (MethodInvocation) right;
						final IMethodBinding binding= invocation.resolveMethodBinding();
						if (binding != null && binding.getName().equals("next")) { //$NON-NLS-1$
							final Expression qualifier= invocation.getExpression();
							if (qualifier instanceof Name) {
								final Name name= (Name) qualifier;
								final IBinding result= name.resolveBinding();
								if (result != null && result.equals(fIterator))
									return visit(left);
							} else if (qualifier instanceof MethodInvocation) {
								final MethodInvocation call= (MethodInvocation) qualifier;
								final IBinding result= call.resolveMethodBinding();
								if (result != null && result.equals(fIterator))
									return visit(left);
							} else if (qualifier instanceof FieldAccess) {
								final FieldAccess access= (FieldAccess) qualifier;
								final IBinding result= access.resolveFieldBinding();
								if (result != null && result.equals(fIterator))
									return visit(left);
							}
						}
					} else if (right instanceof NullLiteral)
						return visit(left);
					return true;
				}

				public final boolean visit(final VariableDeclarationFragment node) {
					return visit(node.getName(), node.getInitializer());
				}
			});
		}
		final ASTNode root= fStatement.getRoot();
		if (root != null) {
			root.accept(new ASTVisitor() {

				public final boolean visit(final ForStatement node) {
					return false;
				}

				public final boolean visit(final SimpleName node) {
					final IBinding binding= node.resolveBinding();
					if (binding != null && binding.equals(fElement))
						fAssigned= true;
					return false;
				}
			});
		}
		return fExpression != null && fIterable != null && fIterator != null && fElement != null && !fAssigned;
	}
}