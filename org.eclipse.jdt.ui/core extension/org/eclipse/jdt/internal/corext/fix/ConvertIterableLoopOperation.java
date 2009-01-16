/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.text.edits.TextEditGroup;

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
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.ImportRemover;
import org.eclipse.jdt.internal.corext.refactoring.util.TightSourceRangeComputer;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;
import org.eclipse.jdt.internal.ui.viewsupport.BasicElementLabels;

/**
 * Operation to convert for loops over iterables to enhanced for loops.
 *
 * @since 3.1
 */
public final class ConvertIterableLoopOperation extends ConvertLoopOperation {

	private static final StatusInfo SEMANTIC_CHANGE_WARNING_STATUS= new StatusInfo(IStatus.WARNING, FixMessages.ConvertIterableLoopOperation_semanticChangeWarning);

	/**
	 * Returns the supertype of the given type with the qualified name.
	 *
	 * @param binding
	 *            the binding of the type
	 * @param name
	 *            the qualified name of the supertype
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
	private final List fOccurrences= new ArrayList(2);

	private EnhancedForStatement fEnhancedForLoop;

	private boolean fMakeFinal;

	public ConvertIterableLoopOperation(ForStatement statement) {
		this(statement, new String[0], false);
	}

	public ConvertIterableLoopOperation(ForStatement statement, String[] usedNames, boolean makeFinal) {
		super(statement, usedNames);
		fMakeFinal= makeFinal;
	}

	public String getIntroducedVariableName() {
		if (fElement != null) {
			return fElement.getName();
		} else {
			return getVariableNameProposals()[0];
		}
	}

	private String[] getVariableNameProposals() {

		String[] variableNames= getUsedVariableNames();
		String[] elementSuggestions= StubUtility.getLocalNameSuggestions(getJavaProject(), FOR_LOOP_ELEMENT_IDENTIFIER, 0, variableNames);

		final ITypeBinding binding= fIterator.getType();
		if (binding != null && binding.isParameterizedType()) {
			String type= binding.getTypeArguments()[0].getName();
			String[] typeSuggestions= StubUtility.getLocalNameSuggestions(getJavaProject(), type, 0, variableNames);

			String[] result= new String[elementSuggestions.length + typeSuggestions.length];
			System.arraycopy(typeSuggestions, 0, result, 0, typeSuggestions.length);
			System.arraycopy(elementSuggestions, 0, result, typeSuggestions.length, elementSuggestions.length);
			return result;
		} else {
			return elementSuggestions;
		}
	}

	private IJavaProject getJavaProject() {
		return getRoot().getJavaElement().getJavaProject();
	}

	private CompilationUnit getRoot() {
		return (CompilationUnit)getForStatement().getRoot();
	}

	/**
	 * Returns the expression for the enhanced for statement.
	 *
	 * @param rewrite
	 *            the AST rewrite to use
	 * @return the expression node, or <code>null</code>
	 */
	private Expression getExpression(final ASTRewrite rewrite) {
		if (fThis)
			return rewrite.getAST().newThisExpression();
		if (fExpression instanceof MethodInvocation)
			return (MethodInvocation)rewrite.createMoveTarget(fExpression);
		return (Expression)ASTNode.copySubtree(rewrite.getAST(), fExpression);
	}

	/**
	 * Returns the iterable type from the iterator type binding.
	 *
	 * @param iterator
	 *            the iterator type binding, or <code>null</code>
	 * @return the iterable type
	 */
	private ITypeBinding getIterableType(final ITypeBinding iterator) {
		if (iterator != null) {
			final ITypeBinding[] bindings= iterator.getTypeArguments();
			if (bindings.length > 0) {
				ITypeBinding arg= bindings[0];
				if (arg.isWildcardType()) {
					arg= ASTResolving.normalizeWildcardType(arg, true, getRoot().getAST());
				}
				return arg;
			}
		}
		return getRoot().getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
	}

	/**
	 * {@inheritDoc}
	 */
	public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel positionGroups) throws CoreException {
		final TextEditGroup group= createTextEditGroup(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description, cuRewrite);

		final ASTRewrite astRewrite= cuRewrite.getASTRewrite();

		TightSourceRangeComputer rangeComputer;
		if (astRewrite.getExtendedSourceRangeComputer() instanceof TightSourceRangeComputer) {
			rangeComputer= (TightSourceRangeComputer)astRewrite.getExtendedSourceRangeComputer();
		} else {
			rangeComputer= new TightSourceRangeComputer();
		}
		rangeComputer.addTightSourceNode(getForStatement());
		astRewrite.setTargetSourceRangeComputer(rangeComputer);

		Statement statement= convert(cuRewrite, group, positionGroups);
		astRewrite.replace(getForStatement(), statement, group);
	}

	protected Statement convert(CompilationUnitRewrite cuRewrite, final TextEditGroup group, final LinkedProposalModel positionGroups) throws CoreException {
		final AST ast= cuRewrite.getAST();
		final ASTRewrite astRewrite= cuRewrite.getASTRewrite();
		final ImportRewrite importRewrite= cuRewrite.getImportRewrite();
		final ImportRemover remover= cuRewrite.getImportRemover();

		fEnhancedForLoop= ast.newEnhancedForStatement();
		String[] names= getVariableNameProposals();

		String name;
		if (fElement != null) {
			name= fElement.getName();
		} else {
			name= names[0];
		}
		final LinkedProposalPositionGroup pg= positionGroups.getPositionGroup(name, true);
		if (fElement != null)
			pg.addProposal(name, null, 10);
		for (int i= 0; i < names.length; i++) {
			pg.addProposal(names[i], null, 10);
		}

		final Statement body= getForStatement().getBody();
		if (body != null) {
			final ListRewrite list;
			if (body instanceof Block) {
				list= astRewrite.getListRewrite(body, Block.STATEMENTS_PROPERTY);
				for (final Iterator iterator= fOccurrences.iterator(); iterator.hasNext();) {
					final Statement parent= (Statement)ASTNodes.getParent((ASTNode)iterator.next(), Statement.class);
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
					pg.addPosition(astRewrite.track(node), false);
					return false;
				}

				public final boolean visit(final MethodInvocation node) {
					final IMethodBinding binding= node.resolveMethodBinding();
					if (binding != null && (binding.getName().equals("next") || binding.getName().equals("nextElement"))) { //$NON-NLS-1$ //$NON-NLS-2$
						final Expression expression= node.getExpression();
						if (expression instanceof Name) {
							final IBinding result= ((Name)expression).resolveBinding();
							if (result != null && result.equals(fIterator))
								return replace(node);
						} else if (expression instanceof FieldAccess) {
							final IBinding result= ((FieldAccess)expression).resolveFieldBinding();
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
							final Statement parent= (Statement)ASTNodes.getParent(node, Statement.class);
							if (parent != null && (list == null || list.getRewrittenList().contains(parent)))
								pg.addPosition(astRewrite.track(node), false);
						}
					}
					return false;
				}
			});

			fEnhancedForLoop.setBody(getBody(cuRewrite, group, positionGroups));
		}
		final SingleVariableDeclaration declaration= ast.newSingleVariableDeclaration();
		final SimpleName simple= ast.newSimpleName(name);
		pg.addPosition(astRewrite.track(simple), true);
		declaration.setName(simple);
		final ITypeBinding iterable= getIterableType(fIterator.getType());
		declaration.setType(importType(iterable, getForStatement(), importRewrite, getRoot()));
		if (fMakeFinal) {
			ModifierRewrite.create(astRewrite, declaration).setModifiers(Modifier.FINAL, 0, group);
		}
		remover.registerAddedImport(iterable.getQualifiedName());
		fEnhancedForLoop.setParameter(declaration);
		fEnhancedForLoop.setExpression(getExpression(astRewrite));

		for (Iterator iterator= getForStatement().initializers().iterator(); iterator.hasNext();) {
			ASTNode node= (ASTNode)iterator.next();
			if (node instanceof VariableDeclarationExpression) {
				VariableDeclarationExpression variableDeclarationExpression= (VariableDeclarationExpression) node;
				remover.registerRemovedNode(variableDeclarationExpression.getType());
			} else {
				remover.registerRemovedNode(node);
			}
		}

		for (Iterator iterator= getForStatement().updaters().iterator(); iterator.hasNext();) {
			ASTNode node= (ASTNode)iterator.next();
			remover.registerRemovedNode(node);
		}

		return fEnhancedForLoop;
	}

	/**
	 * Is this proposal applicable?
	 *
	 * @return A status with severity <code>IStatus.Error</code> if not
	 *         applicable
	 */
	public final IStatus satisfiesPreconditions() {
		IStatus resultStatus= StatusInfo.OK_STATUS;
		if (JavaModelUtil.is50OrHigher(getJavaProject())) {
			resultStatus= checkExpressionCondition();
			if (resultStatus.getSeverity() == IStatus.ERROR)
				return resultStatus;

			List updateExpressions= (List)getForStatement().getStructuralProperty(ForStatement.UPDATERS_PROPERTY);
			if (updateExpressions.size() == 1) {
				resultStatus= new StatusInfo(IStatus.WARNING, Messages.format(FixMessages.ConvertIterableLoopOperation_RemoveUpdateExpression_Warning, BasicElementLabels.getJavaCodeString(((Expression)updateExpressions.get(0)).toString())));
			} else if (updateExpressions.size() > 1) {
				resultStatus= new StatusInfo(IStatus.WARNING, FixMessages.ConvertIterableLoopOperation_RemoveUpdateExpressions_Warning);
			}

			for (final Iterator outer= getForStatement().initializers().iterator(); outer.hasNext();) {
				final Expression initializer= (Expression)outer.next();
				if (initializer instanceof VariableDeclarationExpression) {
					final VariableDeclarationExpression declaration= (VariableDeclarationExpression)initializer;
					List fragments= declaration.fragments();
					if (fragments.size() != 1) {
						return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
					} else {
						final VariableDeclarationFragment fragment= (VariableDeclarationFragment)fragments.get(0);
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
															final Name name= (Name)qualifier;
															fIterable= name.resolveBinding();
														} else if (qualifier instanceof MethodInvocation) {
															final MethodInvocation invocation= (MethodInvocation)qualifier;
															fIterable= invocation.resolveMethodBinding();
														} else if (qualifier instanceof FieldAccess) {
															final FieldAccess access= (FieldAccess)qualifier;
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
			final Statement statement= getForStatement().getBody();
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
									final Name name= (Name)node;
									final IBinding result= name.resolveBinding();
									if (result != null) {
										fOccurrences.add(node);
										fElement= result;
										return false;
									}
								} else if (node instanceof FieldAccess) {
									final FieldAccess access= (FieldAccess)node;
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
						if (fElement != null && left instanceof SimpleName) {
							IBinding binding= ((SimpleName) left).resolveBinding();
							if (fElement.equals(binding))
								fMakeFinal= false;
						}

						if (right instanceof MethodInvocation) {
							final MethodInvocation invocation= (MethodInvocation)right;
							final IMethodBinding binding= invocation.resolveMethodBinding();
							if (binding != null && (binding.getName().equals("next") || binding.getName().equals("nextElement"))) { //$NON-NLS-1$ //$NON-NLS-2$
								final Expression expression= invocation.getExpression();
								if (expression instanceof Name) {
									final Name qualifier= (Name)expression;
									final IBinding result= qualifier.resolveBinding();
									if (result != null && result.equals(fIterator)) {
										nextInvocationCount[0]++;
										return visit(left);
									}
								} else if (expression instanceof FieldAccess) {
									final FieldAccess qualifier= (FieldAccess)expression;
									final IBinding result= qualifier.resolveFieldBinding();
									if (result != null && result.equals(fIterator)) {
										nextInvocationCount[0]++;
										return visit(left);
									}
								}
							} else {
								return visit(invocation);
							}
						}
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
								final Name qualifier= (Name)expression;
								final IBinding result= qualifier.resolveBinding();
								if (result != null && result.equals(fIterator)) {
									if (!binding.getName().equals("next") && !binding.getName().equals("nextElement")) { //$NON-NLS-1$ //$NON-NLS-2$
										otherInvocationThenNext[0]= true;
									} else {
										nextInvocationCount[0]++;
									}
								}
							} else if (expression instanceof FieldAccess) {
								final FieldAccess qualifier= (FieldAccess)expression;
								final IBinding result= qualifier.resolveFieldBinding();
								if (result != null && result.equals(fIterator)) {
									if (!binding.getName().equals("next") && !binding.getName().equals("nextElement")) { //$NON-NLS-1$ //$NON-NLS-2$
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
					return ERROR_STATUS;

				if (nextInvocationCount[0] > 1)
					return ERROR_STATUS;

				if (fElement != null) {
					statement.accept(new ASTVisitor() {
						public final boolean visit(final VariableDeclarationFragment node) {
							if (node.getInitializer() instanceof NullLiteral) {
								SimpleName name= node.getName();
								if (iterable.equals(name.resolveTypeBinding()) && fElement.equals(name.resolveBinding())) {
									fOccurrences.add(name);
								}
							}

							return true;
						}
					});
				}
			}
			final ASTNode root= getForStatement().getRoot();
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
			return ERROR_STATUS;
		}
	}

	private IStatus checkExpressionCondition() {
		Expression expression= getForStatement().getExpression();
		if (!(expression instanceof MethodInvocation))
			return SEMANTIC_CHANGE_WARNING_STATUS;

		MethodInvocation invoc= (MethodInvocation)expression;
		IMethodBinding methodBinding= invoc.resolveMethodBinding();
		if (methodBinding == null)
			return ERROR_STATUS;

		ITypeBinding declaringClass= methodBinding.getDeclaringClass();
		if (declaringClass == null)
			return ERROR_STATUS;

		String qualifiedName= declaringClass.getQualifiedName();
		String methodName= invoc.getName().getIdentifier();
		if (qualifiedName.startsWith("java.util.Enumeration")) { //$NON-NLS-1$
			if (!methodName.equals("hasMoreElements")) //$NON-NLS-1$
				return SEMANTIC_CHANGE_WARNING_STATUS;
		} else if (qualifiedName.startsWith("java.util.Iterator")) { //$NON-NLS-1$
			if (!methodName.equals("hasNext")) //$NON-NLS-1$
				return SEMANTIC_CHANGE_WARNING_STATUS;
			return checkIteratorCondition();
		} else {
			return SEMANTIC_CHANGE_WARNING_STATUS;
		}

		return StatusInfo.OK_STATUS;
	}

	private IStatus checkIteratorCondition() {

		List initializers= getForStatement().initializers();
		if (initializers.size() != 1)
			return SEMANTIC_CHANGE_WARNING_STATUS;

		Expression expression= (Expression)initializers.get(0);
		if (!(expression instanceof VariableDeclarationExpression))
			return SEMANTIC_CHANGE_WARNING_STATUS;

		VariableDeclarationExpression declaration= (VariableDeclarationExpression)expression;
		List variableDeclarationFragments= declaration.fragments();
		if (variableDeclarationFragments.size() != 1)
			return SEMANTIC_CHANGE_WARNING_STATUS;

		Object declarationFragment= variableDeclarationFragments.get(0);
		if (!(declarationFragment instanceof VariableDeclarationFragment))
			return SEMANTIC_CHANGE_WARNING_STATUS;

		Expression initializer= ((VariableDeclarationFragment)declarationFragment).getInitializer();
		if (!(initializer instanceof MethodInvocation))
			return SEMANTIC_CHANGE_WARNING_STATUS;

		MethodInvocation methodInvocation= (MethodInvocation)initializer;
		String methodName= methodInvocation.getName().getIdentifier();
		if (!"iterator".equals(methodName)) //$NON-NLS-1$
			return SEMANTIC_CHANGE_WARNING_STATUS;
		
		return StatusInfo.OK_STATUS;
	}

}
