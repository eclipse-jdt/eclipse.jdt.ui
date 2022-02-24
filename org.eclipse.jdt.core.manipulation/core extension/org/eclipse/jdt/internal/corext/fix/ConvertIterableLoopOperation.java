/*******************************************************************************
 * Copyright (c) 2005, 2022 IBM Corporation and others.
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
 *     Red Hat Inc. - refactored to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Comment;
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
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.ImportRemover;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;

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

		if (binding.getErasure().getQualifiedName().equals(name))
			return binding;

		ITypeBinding type= binding.getSuperclass();
		if (type != null) {
			ITypeBinding result= getSuperType(type, name);
			if (result != null)
				return result;
		}
		for (ITypeBinding type2 : binding.getInterfaces()) {
			ITypeBinding result= getSuperType(type2, name);
			if (result != null)
				return result;
		}
		return null;
	}

	/** Has the element variable been assigned outside the for statement? */
	private boolean fAssigned;

	/** The binding of the element variable */
	private IBinding fElementVariable;

	/** The node of the iterable expression */
	private Expression fExpression;

	/** The type binding of the iterable expression */
	private IBinding fIterable;

	/** Is the iterator method invoked on <code>this</code>? */
	private boolean fThis;

	/** The binding of the iterator variable */
	private IVariableBinding fIteratorVariable;

	/** The nodes of the element variable occurrences */
	private final List<Expression> fOccurrences= new ArrayList<>(2);

	private EnhancedForStatement fEnhancedForLoop;

	private boolean fMakeFinal;

	private boolean fCheckIfVarUsed;

	private boolean fElementVariableReferenced;

	public ConvertIterableLoopOperation(ForStatement statement) {
		this(statement, new String[0], false, false);
	}

	public ConvertIterableLoopOperation(ForStatement statement, String[] usedNames, boolean makeFinal, boolean checkIfVarUsed) {
		super(statement, usedNames);
		fMakeFinal= makeFinal;
		fCheckIfVarUsed= checkIfVarUsed;
	}

	@Override
	public String getIntroducedVariableName() {
		if (fElementVariable != null) {
			return fElementVariable.getName();
		}
		return getVariableNameProposals()[0];
	}

	private String[] getVariableNameProposals() {
		String[] variableNames= getUsedVariableNames();
		String baseName= FOR_LOOP_ELEMENT_IDENTIFIER;
		if (fExpression != null) {
			if  (fExpression instanceof SimpleName) {
				baseName= ConvertLoopOperation.modifybasename(((SimpleName)fExpression).getFullyQualifiedName());
			} else if (fExpression instanceof QualifiedName) {
				baseName= ConvertLoopOperation.modifybasename(((QualifiedName)fExpression).getName().getFullyQualifiedName());
			}
		}
		String[] elementSuggestions= StubUtility.getLocalNameSuggestions(getJavaProject(), baseName, 0, variableNames);

		ITypeBinding binding= fIteratorVariable.getType();
		if (binding != null && binding.isParameterizedType()) {
			String type= binding.getTypeArguments()[0].getName();
			String[] typeSuggestions= StubUtility.getLocalNameSuggestions(getJavaProject(), type, 0, variableNames);

			String[] result= new String[elementSuggestions.length + typeSuggestions.length];
			String[] first= typeSuggestions;
			String[] second= elementSuggestions;
			if (!FOR_LOOP_ELEMENT_IDENTIFIER.equals(baseName)) {
				first= elementSuggestions;
				second= typeSuggestions;
			}
			System.arraycopy(first, 0, result, 0, first.length);
			System.arraycopy(second, 0, result, first.length, second.length);
			return result;
		}
		return elementSuggestions;
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
	 * Returns the type of elements returned by the iterator.
	 *
	 * @param iterator
	 *            the iterator type binding, or <code>null</code>
	 * @return the element type
	 */
	private ITypeBinding getElementType(final ITypeBinding iterator) {
		if (iterator != null) {
			ITypeBinding[] bindings= iterator.getTypeArguments();
			if (bindings.length > 0) {
				ITypeBinding arg= bindings[0];
				if (arg.isWildcardType()) {
					arg= ASTResolving.normalizeWildcardType(arg, true, getRoot().getAST());
				}
				return arg;
			}
		}
		return getRoot().getAST().resolveWellKnownType(Object.class.getCanonicalName());
	}

	@Override
	protected Statement convert(CompilationUnitRewrite cuRewrite, final TextEditGroup group, final LinkedProposalModelCore positionGroups) throws CoreException {
		AST ast= cuRewrite.getAST();
		ASTRewrite astRewrite= cuRewrite.getASTRewrite();

		ImportRewrite importRewrite= cuRewrite.getImportRewrite();
		ImportRemover remover= cuRewrite.getImportRemover();

		fEnhancedForLoop= ast.newEnhancedForStatement();
		String[] names= getVariableNameProposals();

		String name;
		if (fElementVariable != null) {
			name= fElementVariable.getName();
		} else {
			name= names[0];
		}
		LinkedProposalPositionGroupCore pg= positionGroups.getPositionGroup(name, true);
		if (fElementVariable != null)
			pg.addProposal(name, 10);
		for (String name2 : names) {
			pg.addProposal(name2, 10);
		}

		Statement body= getForStatement().getBody();
		List<Comment> commentList= getRoot().getCommentList();
		if (body != null) {
			ListRewrite list;
			if (body instanceof Block) {
				list= astRewrite.getListRewrite(body, Block.STATEMENTS_PROPERTY);
				for (Expression expression : fOccurrences) {
					Statement parent= ASTNodes.getParent(expression, Statement.class);
					if (parent != null && parent.getParent() instanceof Block) {
						ListRewrite innerList= astRewrite.getListRewrite(parent.getParent(), Block.STATEMENTS_PROPERTY);
						List<ASTNode> newComments= new ArrayList<>();
						for (Comment comment : commentList) {
							CompilationUnit cu= (CompilationUnit)parent.getRoot();
							if (comment.getStartPosition() >= cu.getExtendedStartPosition(parent)
									&& comment.getStartPosition() + comment.getLength() < parent.getStartPosition()) {
								String commentString= cuRewrite.getCu().getBuffer().getText(comment.getStartPosition(), comment.getLength());
								ASTNode newComment= astRewrite.createStringPlaceholder(commentString, comment.isBlockComment() ? ASTNode.BLOCK_COMMENT : ASTNode.LINE_COMMENT);
								newComments.add(newComment);
							}
						}
						if (!newComments.isEmpty()) {
							ASTNode lastComment= newComments.get(0);
							innerList.replace(parent, lastComment, group);
							for (int i= 1; i < newComments.size(); ++i) {
								ASTNode nextComment= newComments.get(i);
								innerList.insertAfter(nextComment, lastComment, group);
								lastComment= nextComment;
							}
						} else {
							innerList.remove(parent, group);
						}
						remover.registerRemovedNode(parent);
					}
				}
			} else {
				list= null;
			}
			String text= name;
			body.accept(new ASTVisitor() {

				private boolean replace(final Expression expression) {
					SimpleName node= ast.newSimpleName(text);
					ASTNodes.replaceButKeepComment(astRewrite, expression, node, group);
					remover.registerRemovedNode(expression);
					checkChildOperations(expression, node);
					pg.addPosition(astRewrite.track(node), false);
					return false;
				}

				private void checkChildOperations(Expression newExpression, SimpleName node) {
					ConvertLoopOperation child= getChildLoopOperation();
					while (child != null) {
						if (child instanceof ConvertIterableLoopOperation) {
							Expression exp= ((ConvertIterableLoopOperation) child).getExpression();
							if (newExpression.subtreeMatch(new ASTMatcher(), exp)) {
								((ConvertIterableLoopOperation)child).setExpression(node);
							}
						}
						child= child.getChildLoopOperation();
					}
				}

				@Override
				public final boolean visit(final MethodInvocation node) {
					IMethodBinding binding= node.resolveMethodBinding();
					if (binding != null && ("next".equals(binding.getName()) || "nextElement".equals(binding.getName()))) { //$NON-NLS-1$ //$NON-NLS-2$
						Expression expression= node.getExpression();
						if (expression instanceof Name) {
							IBinding result= ((Name)expression).resolveBinding();
							if (result != null && result.equals(fIteratorVariable))
								return replace(node);
						} else if (expression instanceof FieldAccess) {
							IBinding result= ((FieldAccess)expression).resolveFieldBinding();
							if (result != null && result.equals(fIteratorVariable))
								return replace(node);
						}
					}
					return super.visit(node);
				}

				@Override
				public final boolean visit(final SimpleName node) {
					if (fElementVariable != null) {
						IBinding binding= node.resolveBinding();
						if (binding != null && binding.equals(fElementVariable)) {
							Statement parent= ASTNodes.getParent(node, Statement.class);
							if (parent != null && (list == null || list.getRewrittenList().contains(parent)))
								pg.addPosition(astRewrite.track(node), false);
						}
					}
					return false;
				}
			});

			fEnhancedForLoop.setBody(getBody(cuRewrite, group, positionGroups));
		}
		SingleVariableDeclaration declaration= ast.newSingleVariableDeclaration();
		SimpleName simple= ast.newSimpleName(name);
		pg.addPosition(astRewrite.track(simple), true);
		declaration.setName(simple);
		ITypeBinding elementType= getElementType(fIteratorVariable.getType());
		Type importType= importType(elementType, getForStatement(), importRewrite, getRoot(), TypeLocation.LOCAL_VARIABLE);
		remover.registerAddedImports(importType);
		declaration.setType(importType);
		if (fMakeFinal) {
			ModifierRewrite.create(astRewrite, declaration).setModifiers(Modifier.FINAL, 0, group);
		}
		remover.registerAddedImport(elementType.getQualifiedName());
		fEnhancedForLoop.setParameter(declaration);
		fEnhancedForLoop.setExpression(getExpression(astRewrite));

		for (Object node : getForStatement().initializers()) {
			if (node instanceof VariableDeclarationExpression) {
				VariableDeclarationExpression variableDeclarationExpression= (VariableDeclarationExpression) node;
				remover.registerRemovedNode(variableDeclarationExpression.getType());
			} else {
				remover.registerRemovedNode((ASTNode) node);
			}
		}

		for (Object object : getForStatement().updaters()) {
			remover.registerRemovedNode((ASTNode) object);
		}

		return fEnhancedForLoop;
	}

	public Expression getExpression() {
		return fExpression;
	}

	public void setExpression(Expression exp) {
		fExpression= exp;
	}

	/**
	 * Is this proposal applicable?
	 *
	 * @return A status with severity <code>IStatus.Error</code> if not
	 *         applicable
	 */
	@Override
	public IStatus satisfiesPreconditions() {
		IStatus resultStatus= StatusInfo.OK_STATUS;
		if (JavaModelUtil.is50OrHigher(getJavaProject())) {
			resultStatus= checkExpressionCondition();
			if (resultStatus.getSeverity() == IStatus.ERROR)
				return resultStatus;

			List<Expression> updateExpressions= getForStatement().updaters();
			if (updateExpressions.size() == 1) {
				resultStatus= new StatusInfo(IStatus.WARNING, Messages.format(FixMessages.ConvertIterableLoopOperation_RemoveUpdateExpression_Warning, BasicElementLabels.getJavaCodeString(updateExpressions.get(0).toString())));
			} else if (updateExpressions.size() > 1) {
				resultStatus= new StatusInfo(IStatus.WARNING, FixMessages.ConvertIterableLoopOperation_RemoveUpdateExpressions_Warning);
			}

			for (final Object outer : getForStatement().initializers()) {
				if (outer instanceof VariableDeclarationExpression) {
					VariableDeclarationExpression declaration= (VariableDeclarationExpression) outer;
					List<VariableDeclarationFragment> fragments= declaration.fragments();
					if (fragments.size() != 1) {
						return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
					}
					VariableDeclarationFragment fragment= fragments.get(0);
					IVariableBinding binding= fragment.resolveBinding();
					if (binding != null) {
						ITypeBinding type= binding.getType();
						if (type != null) {
							ITypeBinding iterator= getSuperType(type, Iterator.class.getCanonicalName());

							if (iterator == null) {
								iterator= getSuperType(type, Enumeration.class.getCanonicalName());
							}

							if (iterator != null) {
								fIteratorVariable= binding;

								MethodInvocation method= ASTNodes.as(fragment.getInitializer(), MethodInvocation.class);
								if (method != null && method.resolveMethodBinding() != null) {
									IMethodBinding methodBinding= method.resolveMethodBinding();
									ITypeBinding returnType= methodBinding.getReturnType();
									if (returnType != null) {
										String qualified= returnType.getErasure().getQualifiedName();
										ITypeBinding returnElementType= getElementType(returnType);
										ITypeBinding variableElementType= getElementType(fIteratorVariable.getType());

										if (returnElementType != null
												&& variableElementType != null
												&& returnElementType.isAssignmentCompatible(variableElementType)
												&& ("java.util.Iterator".equals(qualified) || "java.util.Enumeration".equals(qualified))) { //$NON-NLS-1$ //$NON-NLS-2$
											Expression qualifier= method.getExpression();
											if (qualifier != null) {
												ITypeBinding resolved= qualifier.resolveTypeBinding();
												if (resolved != null) {
													ITypeBinding iterable= getSuperType(resolved, Iterable.class.getCanonicalName());
													if (iterable != null) {
														fExpression= qualifier;
														fIterable= resolved;
													}
												}
											} else {
												ITypeBinding declaring= methodBinding.getDeclaringClass();
												if (declaring != null) {
													ITypeBinding superBinding= getSuperType(declaring, Iterable.class.getCanonicalName());
													if (superBinding != null) {
														fIterable= superBinding;
														fThis= true;
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			Statement statement= getForStatement().getBody();
			if (statement != null && fIteratorVariable != null) {
				boolean[] otherInvocationThenNext= { false };
				int[] nextInvocationCount= { 0 };
				ITypeBinding elementType= getElementType(fIteratorVariable.getType());
				statement.accept(new ASTVisitor() {

					@Override
					public boolean visit(SimpleName node) {
						IBinding nodeBinding= node.resolveBinding();
						if (fElementVariable != null && fElementVariable.equals(nodeBinding)) {
							fMakeFinal= false;
						}

						if (nodeBinding == fIteratorVariable) {
							if (node.getLocationInParent() == MethodInvocation.EXPRESSION_PROPERTY) {
								MethodInvocation invocation= (MethodInvocation) node.getParent();
								String name= invocation.getName().getIdentifier();
								if ("next".equals(name) || "nextElement".equals(name)) { //$NON-NLS-1$ //$NON-NLS-2$
									nextInvocationCount[0]++;

									Expression left= null;
									if (invocation.getLocationInParent() == Assignment.RIGHT_HAND_SIDE_PROPERTY) {
										left= ((Assignment) invocation.getParent()).getLeftHandSide();
									} else if (invocation.getLocationInParent() == VariableDeclarationFragment.INITIALIZER_PROPERTY) {
										VariableDeclarationFragment fragment= (VariableDeclarationFragment)invocation.getParent();
										if (fragment.getParent() instanceof VariableDeclarationExpression) {
											VariableDeclarationExpression varexp= (VariableDeclarationExpression)fragment.getParent();
											if (varexp.getLocationInParent() == TryStatement.RESOURCES2_PROPERTY) {
												fElementVariableReferenced= true;
												return true;
											}
										}
										left= fragment.getName();
									}

									return visitElementVariable(left);
								}
							}
							otherInvocationThenNext[0]= true;
						}
						return true;
					}

					private boolean visitElementVariable(final Expression node) {
						if (node != null) {
							ITypeBinding binding= node.resolveTypeBinding();
							if (binding != null && elementType.equals(binding)) {
								if (node instanceof Name) {
									Name name= (Name)node;
									IBinding result= name.resolveBinding();
									if (result != null) {
										fOccurrences.add(node);
										fElementVariable= result;
										return false;
									}
								} else if (node instanceof FieldAccess) {
									FieldAccess access= (FieldAccess)node;
									IBinding result= access.resolveFieldBinding();
									if (result != null) {
										fOccurrences.add(node);
										fElementVariable= result;
										return false;
									}
								}
							}
						}
						return true;
					}
				});
				if (otherInvocationThenNext[0] || nextInvocationCount[0] > 1)
					return ERROR_STATUS;

				if (fElementVariable != null) {
					final String elementVarName= fElementVariable.getName();
					statement.accept(new ASTVisitor() {
						@Override
						public final boolean visit(final VariableDeclarationFragment node) {
							if (node.getInitializer() instanceof NullLiteral) {
								SimpleName name= node.getName();
								if (elementType.equals(name.resolveTypeBinding()) && fElementVariable.equals(name.resolveBinding())) {
									fOccurrences.add(name);
								}
							}

							return true;
						}

						@Override
						public final boolean visit(final SimpleName node) {
							if (fCheckIfVarUsed && !fElementVariableReferenced && node.getFullyQualifiedName().equals(elementVarName)) {
								IBinding binding= node.resolveBinding();
								if (binding != null && binding.equals(fElementVariable)) {
									StructuralPropertyDescriptor location= node.getLocationInParent();
									if (location != VariableDeclarationFragment.NAME_PROPERTY) {
										fElementVariableReferenced= true;
									}
								}
							}
							return true;
						}
					});
				}
			}
			ASTNode root= getForStatement().getRoot();
			if (root != null) {
				root.accept(new ASTVisitor() {

					@Override
					public final boolean visit(final ForStatement node) {
						return !node.equals(getForStatement());
					}

					@Override
					public final boolean visit(final SimpleName node) {
						IBinding binding= node.resolveBinding();
						if (binding != null && binding.equals(fElementVariable))
							fAssigned= true;
						return false;
					}
				});
			}
		}
		if ((fExpression != null || fThis) && fIterable != null && fIteratorVariable != null && !fAssigned && (fElementVariableReferenced || !fCheckIfVarUsed)) {
			return resultStatus;
		}
		return ERROR_STATUS;
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

		String qualifiedName= declaringClass.getErasure().getQualifiedName();
		String methodName= invoc.getName().getIdentifier();
		if (Enumeration.class.getCanonicalName().equals(qualifiedName)) {
			if (!"hasMoreElements".equals(methodName)) //$NON-NLS-1$
				return SEMANTIC_CHANGE_WARNING_STATUS;
		} else if (Iterator.class.getCanonicalName().equals(qualifiedName)) {
			if (!"hasNext".equals(methodName)) //$NON-NLS-1$
				return SEMANTIC_CHANGE_WARNING_STATUS;
			return checkIteratorCondition();
		} else {
			return SEMANTIC_CHANGE_WARNING_STATUS;
		}

		return StatusInfo.OK_STATUS;
	}

	private IStatus checkIteratorCondition() {
		List<Expression> initializers= getForStatement().initializers();
		if (initializers.size() != 1)
			return SEMANTIC_CHANGE_WARNING_STATUS;

		Expression expression= initializers.get(0);
		if (!(expression instanceof VariableDeclarationExpression))
			return SEMANTIC_CHANGE_WARNING_STATUS;

		VariableDeclarationExpression declaration= (VariableDeclarationExpression)expression;
		List<VariableDeclarationFragment> variableDeclarationFragments= declaration.fragments();
		if (variableDeclarationFragments.size() != 1)
			return SEMANTIC_CHANGE_WARNING_STATUS;

		VariableDeclarationFragment declarationFragment= variableDeclarationFragments.get(0);

		Expression initializer= declarationFragment.getInitializer();
		if (!(initializer instanceof MethodInvocation))
			return SEMANTIC_CHANGE_WARNING_STATUS;

		MethodInvocation methodInvocation= (MethodInvocation)initializer;
		String methodName= methodInvocation.getName().getIdentifier();
		if (!"iterator".equals(methodName) || !methodInvocation.arguments().isEmpty()) //$NON-NLS-1$
			return SEMANTIC_CHANGE_WARNING_STATUS;

		return StatusInfo.OK_STATUS;
	}
}
