/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
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
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.fix.LinkedFix.AbstractLinkedFixRewriteOperation;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.ImportRemover;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;

/**
 * Operation to convert for loops over iterables to enhanced for loops.
 * 
 * @since 3.1
 */
public final class ConvertIterableLoopOperation extends AbstractLinkedFixRewriteOperation {

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

	/** The binding of the element variable */
	private IBinding fElement= null;

	/** The node of the iterable object used in the expression */
	private Expression fExpression= null;

	/** The binding of the iterable object */
	private IBinding fIterable= null;

	/** Is the iterator method invoked on <code>this</code>? */
	private boolean fThis= false;

	/** The binding of the iterator variable */
	private IVariableBinding fIterator= null;

	/** The nodes of the element variable occurrences */
	private List fOccurrences= new ArrayList(2);

	/** The compilation unit to operate on */
	private final CompilationUnit fRoot;

	/** The for statement to convert */
	private final ForStatement fStatement;

	private final ICompilationUnit fCompilationUnit;

	private final String fIdentifierName;

	private EnhancedForStatement fEnhancedForLoop;
	
	private boolean fPassive;

	/**
	 * Creates a new convert iterable loop proposal.
	 * 
	 * @param unit the compilation unit containing the for statement
	 * @param statement the for statement to be converted
	 */
	public ConvertIterableLoopOperation(final CompilationUnit unit, final ForStatement statement, String identifierName) {
		fIdentifierName= identifierName;
		fCompilationUnit= (ICompilationUnit)unit.getJavaElement();
		fStatement= statement;
		fRoot= (CompilationUnit) statement.getRoot();
		fPassive= false;
	}

    public ForStatement getForLoop() {
	    return fStatement;
    }
	
	public EnhancedForStatement getEnhancedForStatement() {
		return fEnhancedForLoop;
	}
	
    public void makePassive() {
    }

	private List computeElementNames() {
		final List names= new ArrayList();
		final IJavaProject project= fCompilationUnit.getJavaProject();
		String name= fIdentifierName;
		final ITypeBinding binding= fIterator.getType();
		if (binding != null && binding.isParameterizedType())
			name= binding.getTypeArguments()[0].getName();
		final List excluded= getExcludedNames();
		final String[] suggestions= StubUtility.getLocalNameSuggestions(project, name, 0, (String[]) excluded.toArray(new String[excluded.size()]));
		for (int index= 0; index < suggestions.length; index++)
			names.add(suggestions[index]);
		return names;
	}

	private List getExcludedNames() {
		final CompilationUnit unit= (CompilationUnit) fStatement.getRoot();
		final IBinding[] before= (new ScopeAnalyzer(unit)).getDeclarationsInScope(fStatement.getStartPosition(), ScopeAnalyzer.VARIABLES);
		final IBinding[] after= (new ScopeAnalyzer(unit)).getDeclarationsAfter(fStatement.getStartPosition() + fStatement.getLength(), ScopeAnalyzer.VARIABLES);
		final List names= new ArrayList();
		for (int index= 0; index < before.length; index++)
			names.add(before[index].getName());
		for (int index= 0; index < after.length; index++)
			names.add(after[index].getName());
		return names;
	}

	/**
	 * Returns the expression for the enhanced for statement.
	 * 
	 * @param rewrite the AST rewrite to use
	 * @return the expression node, or <code>null</code>
	 */
	private Expression getExpression(final ASTRewrite rewrite) {
		if (fThis)
			return rewrite.getAST().newThisExpression();
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
			if (bindings.length > 0) {
				ITypeBinding arg= bindings[0];
				if (arg.isWildcardType()) {
					arg= ASTResolving.normalizeWildcardType(arg, true, fRoot.getAST());
				}
				return arg;
			}
		}
		return fRoot.getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
	}

	private ITrackedNodePosition rewriteAST(final AST ast, final ASTRewrite astRewrite, final ImportRewrite importRewrite, final TextEditGroup group) throws CoreException {
		final ImportRemover remover= new ImportRemover(fCompilationUnit.getJavaProject(), (CompilationUnit) fStatement.getRoot());
		
		fEnhancedForLoop= ast.newEnhancedForStatement();
		final List names= computeElementNames();
		String name= fIdentifierName;
		if (fElement != null) {
			name= fElement.getName();
			if (!names.contains(name))
				names.add(0, name);
		} else {
			if (!names.isEmpty())
				name= (String) names.get(0);
		}
		for (final Iterator iterator= names.iterator(); iterator.hasNext();)
			getPositionGroup(fIdentifierName).addProposal((String) iterator.next(), null);
		
		final Statement body= fStatement.getBody();
		if (body != null) {
			final ListRewrite list;
			if (body instanceof Block) {
				list= astRewrite.getListRewrite(body, Block.STATEMENTS_PROPERTY);
				for (final Iterator iterator= fOccurrences.iterator(); iterator.hasNext();) {
					final Statement parent= (Statement) ASTNodes.getParent((ASTNode) iterator.next(), Statement.class);
					if (parent != null && list.getRewrittenList().contains(parent)) {
						list.remove(parent, null);
						remover.registerRemovedNode(parent);
					}
				}
			} else {
				list= null;
			}
			final String text= name;
			body.accept(new ASTVisitor() {

				private boolean replace(final Expression expression) {
					final SimpleName node= ast.newSimpleName(text);
					astRewrite.replace(expression, node, group);
					remover.registerRemovedNode(expression);
					getPositionGroup(fIdentifierName).addPosition(astRewrite.track(node));
					return false;
				}

				public final boolean visit(final MethodInvocation node) {
					final IMethodBinding binding= node.resolveMethodBinding();
					if (binding != null && (binding.getName().equals("next") || binding.getName().equals("nextElement"))) { //$NON-NLS-1$ //$NON-NLS-2$
						final Expression expression= node.getExpression();
						if (expression instanceof Name) {
							final IBinding result= ((Name) expression).resolveBinding();
							if (result != null && result.equals(fIterator))
								return replace(node);
						} else if (expression instanceof FieldAccess) {
							final IBinding result= ((FieldAccess) expression).resolveFieldBinding();
							if (result != null && result.equals(fIterator))
								return replace(node);
						}
					}
					return super.visit(node);
				}

				public final boolean visit(final SimpleName node) {
					if (fElement != null) {
						final IBinding binding= node.resolveBinding();
						if (binding != null && binding.equals(fElement)) {
							final Statement parent= (Statement) ASTNodes.getParent(node, Statement.class);
							if (parent != null && (list == null || list.getRewrittenList().contains(parent)))
								getPositionGroup(fIdentifierName).addPosition(astRewrite.track(node));
						}
					}
					return false;
				}
			});
			fEnhancedForLoop.setBody((Statement) astRewrite.createMoveTarget(body));
		}
		final SingleVariableDeclaration declaration= ast.newSingleVariableDeclaration();
		final SimpleName simple= ast.newSimpleName(name);
		getPositionGroup(fIdentifierName).addFirstPosition(astRewrite.track(simple));
		declaration.setName(simple);
		final ITypeBinding iterable= getIterableType(fIterator.getType());
		final ImportRewrite imports= importRewrite;
		declaration.setType(importType(iterable, fStatement, importRewrite, fRoot));
		remover.registerAddedImport(iterable.getQualifiedName());
		fEnhancedForLoop.setParameter(declaration);
		fEnhancedForLoop.setExpression(getExpression(astRewrite));
		if (!fPassive)
			astRewrite.replace(fStatement, fEnhancedForLoop, group);
		
		remover.registerRemovedNode(fStatement);
		remover.applyRemoves(imports);
		return null;
	}

	/**
	 * Is this proposal applicable?
	 * 
	 * @return A status with severity <code>IStatus.Error</code> if not applicable
	 */
	public final IStatus isApplicable() {
		IStatus resultStatus= StatusInfo.OK_STATUS;
		if (JavaModelUtil.is50OrHigher(fCompilationUnit.getJavaProject())) {
			resultStatus= checkExpressionCondition();
			if (resultStatus.getSeverity() == IStatus.ERROR)
				return resultStatus;
			
			for (final Iterator outer= fStatement.initializers().iterator(); outer.hasNext();) {
				final Expression initializer= (Expression) outer.next();
				if (initializer instanceof VariableDeclarationExpression) {
					final VariableDeclarationExpression declaration= (VariableDeclarationExpression) initializer;
					for (Iterator inner= declaration.fragments().iterator(); inner.hasNext();) {
						final VariableDeclarationFragment fragment= (VariableDeclarationFragment) inner.next();
						fragment.accept(new ASTVisitor() {

							public final boolean visit(final MethodInvocation node) {
								final IMethodBinding binding= node.resolveMethodBinding();
								if (binding != null) {
									final ITypeBinding type= binding.getReturnType();
									if (type != null) {
										final String qualified= type.getQualifiedName();
										if (qualified.startsWith("java.util.Enumeration<") || qualified.startsWith("java.util.Iterator<")) { //$NON-NLS-1$ //$NON-NLS-2$
											final Expression qualifier= node.getExpression();
											if (qualifier != null) {
												final ITypeBinding resolved= qualifier.resolveTypeBinding();
												if (resolved != null) {
													final ITypeBinding iterable= getSuperType(resolved, "java.lang.Iterable"); //$NON-NLS-1$
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
														} else if (qualifier instanceof ThisExpression)
															fIterable= resolved;
													}
												}
											} else {
												final ITypeBinding declaring= binding.getDeclaringClass();
												if (declaring != null) {
													final ITypeBinding superBinding= getSuperType(declaring, "java.lang.Iterable"); //$NON-NLS-1$
													if (superBinding != null) {
														fIterable= superBinding;
														fThis= true;
													}
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
										ITypeBinding iterator= getSuperType(type, "java.util.Iterator"); //$NON-NLS-1$
										if (iterator != null)
											fIterator= binding;
										else {
											iterator= getSuperType(type, "java.util.Enumeration"); //$NON-NLS-1$
											if (iterator != null)
												fIterator= binding;
										}
									}
								}
								return true;
							}
						});
					}
				}
			}
			final Statement statement= fStatement.getBody();
			final boolean[] otherInvocationThenNext= new boolean[] {false};
			final int[] nextInvocationCount= new int[] {0};
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
							if (binding != null && (binding.getName().equals("next") || binding.getName().equals("nextElement"))) { //$NON-NLS-1$ //$NON-NLS-2$
								final Expression expression= invocation.getExpression();
								if (expression instanceof Name) {
									final Name qualifier= (Name) expression;
									final IBinding result= qualifier.resolveBinding();
									if (result != null && result.equals(fIterator)) {
										nextInvocationCount[0]++;
										return visit(left);	
									}
								} else if (expression instanceof FieldAccess) {
									final FieldAccess qualifier= (FieldAccess) expression;
									final IBinding result= qualifier.resolveFieldBinding();
									if (result != null && result.equals(fIterator)) {
										nextInvocationCount[0]++;
										return visit(left);
									}
								}
							} else {
								return visit(invocation);
							}
						} else if (right instanceof NullLiteral)
							return visit(left);
						return true;
					}
					
					/**
					 * {@inheritDoc}
					 */
					public boolean visit(MethodInvocation invocation) {
						final IMethodBinding binding= invocation.resolveMethodBinding();
						if (binding != null) {
							final Expression expression= invocation.getExpression();
							if (expression instanceof Name) {
								final Name qualifier= (Name) expression;
								final IBinding result= qualifier.resolveBinding();
								if (result != null && result.equals(fIterator)) {
									if (!binding.getName().equals("next") && !binding.getName().equals("nextElement")) {  //$NON-NLS-1$ //$NON-NLS-2$
										otherInvocationThenNext[0]= true;
									} else {
										nextInvocationCount[0]++;
									}
								}
							} else if (expression instanceof FieldAccess) {
								final FieldAccess qualifier= (FieldAccess) expression;
								final IBinding result= qualifier.resolveFieldBinding();
								if (result != null && result.equals(fIterator)) {
									if (!binding.getName().equals("next") && !binding.getName().equals("nextElement")) {  //$NON-NLS-1$ //$NON-NLS-2$
										otherInvocationThenNext[0]= true;
									} else {
										nextInvocationCount[0]++;
									}
								}
							}
						}
						return false;
					}

					public final boolean visit(final VariableDeclarationFragment node) {
						return visit(node.getName(), node.getInitializer());
					}
				});
				if (otherInvocationThenNext[0])
					return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
				
				if (nextInvocationCount[0] > 1)
					return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
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
		}
		if ((fExpression != null || fThis) && fIterable != null && fIterator != null && !fAssigned) {
			return resultStatus;
		} else {
			return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
		}
	}

    private IStatus checkExpressionCondition() {
		String warningLable= FixMessages.ConvertIterableLoopOperation_semanticChangeWarning;
		
    	Expression expression= fStatement.getExpression();
		if (!(expression instanceof MethodInvocation))
			return new StatusInfo(IStatus.WARNING, warningLable);
		
		MethodInvocation invoc= (MethodInvocation)expression;
		IMethodBinding methodBinding= invoc.resolveMethodBinding();
		if (methodBinding == null)
			return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
		
		ITypeBinding declaringClass= methodBinding.getDeclaringClass();
		if (declaringClass == null)
			return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
		
		String qualifiedName= declaringClass.getQualifiedName();
		String methodName= invoc.getName().getIdentifier();
		if (qualifiedName.startsWith("java.util.Enumeration")) { //$NON-NLS-1$
			if (!methodName.equals("hasMoreElements")) //$NON-NLS-1$
				return new StatusInfo(IStatus.WARNING, warningLable);
		} else if (qualifiedName.startsWith("java.util.Iterator")) { //$NON-NLS-1$
			if (!methodName.equals("hasNext")) //$NON-NLS-1$
				return new StatusInfo(IStatus.WARNING, warningLable);
		} else {
			return new StatusInfo(IStatus.WARNING, warningLable);
		}
		
	    return StatusInfo.OK_STATUS;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.fix.LinkedFix.ILinkedFixRewriteOperation#rewriteAST(org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite, java.util.List, java.util.List)
	 */
	public ITrackedNodePosition rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups, List positionGroups) throws CoreException {
		TextEditGroup group= createTextEditGroup(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description);
		textEditGroups.add(group);
		clearPositionGroups();
		ITrackedNodePosition endPosition= rewriteAST(cuRewrite.getRoot().getAST(), cuRewrite.getASTRewrite(), cuRewrite.getImportRewrite(), group);
		positionGroups.addAll(getAllPositionGroups());
		return endPosition;
	}

}
