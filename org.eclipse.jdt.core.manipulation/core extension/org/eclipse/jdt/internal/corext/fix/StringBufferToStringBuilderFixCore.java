/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.MethodRefParameter;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.AbortSearchException;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;
import org.eclipse.jdt.internal.ui.util.ASTHelper;

public class StringBufferToStringBuilderFixCore extends CompilationUnitRewriteOperationsFixCore {

	private static final String STRINGBUFFER_NAME= StringBuffer.class.getCanonicalName();
	private static final String OBJECT_NAME= Object.class.getCanonicalName();

	private static boolean isStringBufferType(ITypeBinding typeBinding) {
		if (typeBinding == null) {
			throw new AbortSearchException();
		}
		if (typeBinding.isArray()) {
			typeBinding= typeBinding.getElementType();
		}
		return typeBinding.getQualifiedName().equals(STRINGBUFFER_NAME);
	}

	private static boolean isObjectType(ITypeBinding typeBinding) {
		if (typeBinding == null) {
			throw new AbortSearchException();
		}
		if (typeBinding.isArray()) {
			typeBinding= typeBinding.getElementType();
		}
		return typeBinding.getQualifiedName().equals(OBJECT_NAME);
	}

	public static final class StringBufferFinder extends ASTVisitor {

		private final List<CompilationUnitRewriteOperation> fOperations;

		public StringBufferFinder(List<CompilationUnitRewriteOperation> operations) {
			super(true);
			fOperations= operations;
		}

		private void checkType(Type type) {
			if (type instanceof ArrayType) {
				type= ((ArrayType)type).getElementType();
			}
			ITypeBinding typeBinding= type.resolveBinding();
			if (typeBinding != null && typeBinding.getQualifiedName().equals(STRINGBUFFER_NAME)) {
				fOperations.add(new ChangeStringBufferToStringBuilder(type));
			}
		}

		@Override
		public boolean visit(final VariableDeclarationStatement visited) {
			Type type= visited.getType();
			checkType(type);
			return true;
		}

		@Override
		public boolean visit(final FieldDeclaration visited) {
			Type type= visited.getType();
			checkType(type);
			return true;
		}

		@Override
		public boolean visit(final MethodDeclaration visited) {
			IMethodBinding methodBinding= visited.resolveBinding();
			if (methodBinding == null) {
				return true;
			}
			Type returnType= visited.getReturnType2();
			if (returnType != null) {
				checkType(returnType);
			}
			for (Object obj : visited.parameters()) {
				SingleVariableDeclaration parm= (SingleVariableDeclaration)obj;
				Type type= parm.getType();
				checkType(type);
			}
			return true;
		}

		@Override
		public boolean visit(final ClassInstanceCreation visited) {
			Type type= visited.getType();
			checkType(type);
			return true;
		}

		@Override
		public boolean visit(final CastExpression visited) {
			Type type= visited.getType();
			checkType(type);
			return true;
		}

		@Override
		public boolean visit(final ArrayCreation visited) {
			Type type= visited.getType();
			checkType(type);
			return true;
		}

		@Override
		public boolean visit(final ParameterizedType visited) {
			List<Type> types= visited.typeArguments();
			for (Type type : types) {
				checkType(type);
			}
			return true;
		}

		@Override
		public boolean visit(final TypeDeclaration visited) {
			List<TypeParameter> typeParameters= visited.typeParameters();
			for (TypeParameter typeParameter : typeParameters) {
				List<Type> types= typeParameter.typeBounds();
				for (Type type : types) {
					checkType(type);
				}
			}
			Type superClassType= visited.getSuperclassType();
			if (superClassType != null) {
				checkType(superClassType);
			}
			if (ASTHelper.isSealedTypeSupportedInAST(visited.getAST())) {
				List<Type> permittedTypes= visited.permittedTypes();
				for (Type type : permittedTypes) {
					checkType(type);
				}
			}
			return true;
		}

		@Override
		public boolean visit(final RecordDeclaration visited) {
			List<TypeParameter> typeParameters= visited.typeParameters();
			for (TypeParameter typeParameter : typeParameters) {
				List<Type> types= typeParameter.typeBounds();
				for (Type type : types) {
					checkType(type);
				}
			}
			return true;
		}

		@Override
		public boolean visit(final SingleVariableDeclaration visited) {
			Type type= visited.getType();
			checkType(type);
			return true;
		}

		@Override
		public boolean visit(final EnhancedForStatement visited) {
			return true;
		}

		@Override
		public boolean visit(final InstanceofExpression visited) {
			Type type= visited.getRightOperand();
			checkType(type);
			return true;
		}

		@Override
		public boolean visit(final MethodRefParameter visited) {
			Type type= visited.getType();
			checkType(type);
			return true;
		}

		@Override
		public boolean visit(final MethodRef visited) {
			Name name= visited.getQualifier();
			if (name instanceof SimpleName && name.getFullyQualifiedName().equals(StringBuffer.class.getSimpleName())) {
				fOperations.add(new ChangeStringBufferToStringBuilder((SimpleName)name));
			}
			return true;
		}

		@Override
		public boolean visit(final TagElement visited) {
			List<ASTNode> fragments= visited.fragments();
			for (ASTNode fragment : fragments) {
				if (fragment instanceof SimpleName && ((SimpleName)fragment).getFullyQualifiedName().equals(StringBuffer.class.getSimpleName())) {
					fOperations.add(new ChangeStringBufferToStringBuilder((SimpleName)fragment));
				}
			}
			return true;
		}
	}

	public static final class ValidLocalMethodFinder extends ASTVisitor {

		private final List<MethodDeclaration> fMethodDeclarations;

		public ValidLocalMethodFinder(List<MethodDeclaration> methodDeclarations) {
			fMethodDeclarations= methodDeclarations;
		}

		@Override
		public boolean visit(MethodDeclaration visited) {
			IMethodBinding methodBinding= visited.resolveBinding();
			if (methodBinding != null) {
				ASTVisitor checkMethodDeclaration= new ASTVisitor() {

					class CheckNodeForValidReferences {

						private final ASTNode fASTNode;
						private final boolean fLocalVarsOnly;

						public CheckNodeForValidReferences(ASTNode node, boolean localVarsOnly) {
							fASTNode= node;
							fLocalVarsOnly= localVarsOnly;
						}

						public boolean isValid() {
							ASTVisitor visitor= new ASTVisitor() {


								@Override
								public boolean visit(FieldAccess visitedField) {
									IVariableBinding binding= visitedField.resolveFieldBinding();
									if (binding == null) {
										throw new AbortSearchException();
									}
									if (fLocalVarsOnly && visitedField.getLocationInParent() == MethodInvocation.EXPRESSION_PROPERTY) {
										MethodInvocation methodInvocation= (MethodInvocation)visitedField.getParent();
										IMethodBinding methodInvocationBinding= methodInvocation.resolveMethodBinding();
										if (methodInvocationBinding == null) {
											throw new AbortSearchException();
										}
										ITypeBinding methodTypeBinding= methodInvocationBinding.getReturnType();
										if (isStringBufferType(methodTypeBinding)) {
											throw new AbortSearchException();
										}
									}
									return true;
								}

								@Override
								public boolean visit(SuperFieldAccess visitedField) {
									IVariableBinding binding= visitedField.resolveFieldBinding();
									if (binding == null) {
										throw new AbortSearchException();
									}
									if (fLocalVarsOnly && visitedField.getLocationInParent() == MethodInvocation.EXPRESSION_PROPERTY) {
										MethodInvocation methodInvocation= (MethodInvocation)visitedField.getParent();
										IMethodBinding methodInvocationBinding= methodInvocation.resolveMethodBinding();
										if (methodInvocationBinding == null) {
											throw new AbortSearchException();
										}
										ITypeBinding methodTypeBinding= methodInvocationBinding.getReturnType();
										if (isStringBufferType(methodTypeBinding)) {
											throw new AbortSearchException();
										}
									}
									return true;
								}

								@Override
								public boolean visit(MethodInvocation methodInvocation) {
									if (fLocalVarsOnly) {
										IMethodBinding methodInvocationBinding= methodInvocation.resolveMethodBinding();
										if (methodInvocationBinding == null) {
											throw new AbortSearchException();
										}
										ITypeBinding methodTypeBinding= methodInvocationBinding.getReturnType();
										if (isStringBufferType(methodTypeBinding)) {
											Expression exp= methodInvocation.getExpression();
											if (exp instanceof SimpleName) {
												IBinding binding= ((SimpleName)exp).resolveBinding();
												if (binding instanceof IVariableBinding &&
														!((IVariableBinding)binding).isField() &&
														!((IVariableBinding)binding).isParameter() &&
														!((IVariableBinding)binding).isRecordComponent()) {
													ITypeBinding typeBinding= ((IVariableBinding)binding).getType();
													if (isStringBufferType(typeBinding)) {
														return true;
													}
												}
											}
											throw new AbortSearchException();
										}
									}
									return true;
								}

								@Override
								public boolean visit(CastExpression castExpression) {
									Type castType= castExpression.getType();
									ITypeBinding typeBinding= castType.resolveBinding();
									if (isStringBufferType(typeBinding)) {
										Expression exp= castExpression.getExpression();
										if (exp instanceof Name) {
											IBinding binding= ((Name)exp).resolveBinding();
											if (binding instanceof IVariableBinding) {
												IVariableBinding simpleNameVarBinding= (IVariableBinding)binding;
												if (!fLocalVarsOnly) {
													if (!simpleNameVarBinding.isField() && !simpleNameVarBinding.isParameter()
															&& !simpleNameVarBinding.isRecordComponent()) {
														throw new AbortSearchException();
													}
												} else {
													if (simpleNameVarBinding.isField() || simpleNameVarBinding.isParameter()
															|| simpleNameVarBinding.isRecordComponent()) {
														throw new AbortSearchException();
													}
												}
											}
										}
										throw new AbortSearchException();
									}
									return true;
								}

								@Override
								public boolean visit(SimpleName simpleName) {
									IBinding simpleNameBinding= simpleName.resolveBinding();
									if (simpleNameBinding == null) {
										throw new AbortSearchException();
									}
									if (simpleNameBinding instanceof IVariableBinding) {
										IVariableBinding simpleNameVarBinding= (IVariableBinding)simpleNameBinding;
										ITypeBinding simpleNameTypeBinding= simpleNameVarBinding.getType();
										if (isStringBufferType(simpleNameTypeBinding)) {
											if (simpleName.getLocationInParent() == MethodInvocation.EXPRESSION_PROPERTY) {
												MethodInvocation methodInvocation= (MethodInvocation)simpleName.getParent();
												IMethodBinding methodInvocationBinding= methodInvocation.resolveMethodBinding();
												if (methodInvocationBinding == null) {
													throw new AbortSearchException();
												}
												ITypeBinding methodInvocationReturnType= methodInvocationBinding.getReturnType();
												if (!isStringBufferType(methodInvocationReturnType)) {
													return true;
												}
											}
											if (!fLocalVarsOnly) {
												if (!simpleNameVarBinding.isField() && !simpleNameVarBinding.isParameter()
														&& !simpleNameVarBinding.isRecordComponent()) {
													throw new AbortSearchException();
												}
											} else {
												if (simpleNameVarBinding.isField() || simpleNameVarBinding.isParameter()
														|| simpleNameVarBinding.isRecordComponent()) {
													throw new AbortSearchException();
												}
											}
										}
									}
									return true;
								}
							};
							try {
								fASTNode.accept(visitor);
								return true;
							} catch (AbortSearchException e) {
								// do nothing and fall through
							}
							return false;
						}

					}

					@Override
					public boolean visit(MethodInvocation methodInvocation) {
						IMethodBinding methodInvocationBinding= methodInvocation.resolveMethodBinding();
						if (methodInvocationBinding == null) {
							throw new AbortSearchException();
						}
						Expression exp= ASTNodes.getUnparenthesedExpression(methodInvocation.getExpression());
						while (exp instanceof MethodInvocation) {
							exp= ((MethodInvocation)exp).getExpression();
						}
						if (exp instanceof SimpleName) {
							// Allow local variable StringBuffer method calls
							IBinding expBinding= ((SimpleName)exp).resolveBinding();
							if (expBinding instanceof IVariableBinding) {
								IVariableBinding expVarBinding= (IVariableBinding)expBinding;
								if (!expVarBinding.isField() && !expVarBinding.isParameter() && !expVarBinding.isRecordComponent()) {
									ITypeBinding typeBinding= expVarBinding.getType();
									if (isStringBufferType(typeBinding)) {
										return true;
									}
								}
							}
						}
						return checkMethodArgs(methodInvocation.arguments(), methodInvocationBinding);
					}

					@Override
					public boolean visit(SuperMethodInvocation superMethodInvocation) {
						IMethodBinding methodInvocationBinding= superMethodInvocation.resolveMethodBinding();
						if (methodInvocationBinding == null) {
							throw new AbortSearchException();
						}
						return checkMethodArgs(superMethodInvocation.arguments(), methodInvocationBinding);
					}

					@Override
					public boolean visit(ConstructorInvocation constructorInvocation) {
						IMethodBinding constructorBinding= constructorInvocation.resolveConstructorBinding();
						if (constructorBinding == null) {
							throw new AbortSearchException();
						}
						return checkMethodArgs(constructorInvocation.arguments(), constructorBinding);
					}

					@Override
					public boolean visit(ClassInstanceCreation classInstanceCreation) {
						IMethodBinding constructorBinding= classInstanceCreation.resolveConstructorBinding();
						if (constructorBinding == null) {
							throw new AbortSearchException();
						}
						return checkMethodArgs(classInstanceCreation.arguments(), constructorBinding);
					}

					private boolean checkMethodArgs(List<Expression> methodArgs, IMethodBinding methodInvocationBinding) {
						ITypeBinding[] parameterTypes= methodInvocationBinding.getParameterTypes();
						int len= methodArgs.size() <= parameterTypes.length ? methodArgs.size() : parameterTypes.length;
						for (int i= 0; i < len; ++i) {
							ITypeBinding methodArgBinding= methodArgs.get(i).resolveTypeBinding();
							if (isStringBufferType(parameterTypes[i]) ||
									(isStringBufferType(methodArgBinding) && isObjectType(parameterTypes[i]))) {
								CheckNodeForValidReferences checkNode= new CheckNodeForValidReferences(methodArgs.get(i), false);
								if (checkNode.isValid()) {
									continue;
								}
								// otherwise, punt in case a local variable gets passed
								throw new AbortSearchException();
							}
						}
						if (methodInvocationBinding.isVarargs() && methodArgs.size() > parameterTypes.length) {
							for (int i= parameterTypes.length; i < methodArgs.size(); ++i) {
								ITypeBinding methodArgBinding= methodArgs.get(i).resolveTypeBinding();
								if (isStringBufferType(methodArgBinding)) {
									CheckNodeForValidReferences checkNode= new CheckNodeForValidReferences(methodArgs.get(i), false);
									if (checkNode.isValid()) {
										continue;
									}
									// otherwise, punt in case a local variable gets passed
									throw new AbortSearchException();
								}
							}
						}
						return true;
					}

					@Override
					public boolean visit(VariableDeclarationStatement varDeclStatement) {
						Type varDeclType= varDeclStatement.getType();
						ITypeBinding varDeclTypeBinding= varDeclType.resolveBinding();
						if (isStringBufferType(varDeclTypeBinding)) {
							List<VariableDeclarationFragment> frags= varDeclStatement.fragments();
							for (VariableDeclarationFragment frag : frags) {
								Expression initializer= frag.getInitializer();
								if (initializer != null) {
									CheckNodeForValidReferences checkNode= new CheckNodeForValidReferences(initializer, true);
									if (!checkNode.isValid()) {
										throw new AbortSearchException();
									}
								}
							}
						}
						return true;
					}

					@Override
					public boolean visit(Assignment assignment) {
						ITypeBinding assignmentTypeBinding= assignment.resolveTypeBinding();
						if (isStringBufferType(assignmentTypeBinding)) {
							Expression leftSide= assignment.getLeftHandSide();
							if (leftSide instanceof ArrayAccess) {
								while (leftSide instanceof ArrayAccess) {
									leftSide= ((ArrayAccess)leftSide).getArray();
								}
							}
							IBinding leftSideBinding= null;
							if (leftSide instanceof Name) {
								leftSideBinding= ((Name)leftSide).resolveBinding();
							} else if (leftSide instanceof SuperFieldAccess) {
								leftSideBinding= ((SuperFieldAccess)leftSide).resolveFieldBinding();
							} else if (leftSide instanceof FieldAccess) {
								leftSideBinding= ((FieldAccess)leftSide).resolveFieldBinding();
							}
							if (leftSideBinding != null) {
								if (leftSideBinding instanceof IVariableBinding) {
									IVariableBinding leftSideVarBinding= (IVariableBinding)leftSideBinding;
									CheckNodeForValidReferences checkNode=
											new CheckNodeForValidReferences(assignment.getRightHandSide(),
													!leftSideVarBinding.isField() && !leftSideVarBinding.isParameter()
													&& !leftSideVarBinding.isRecordComponent());
									if (checkNode.isValid()) {
										return true;
									}
								}
							}
							throw new AbortSearchException();
						}
						return true;
					}

					@Override
					public boolean visit(final EnhancedForStatement enhancedFor) {
						SingleVariableDeclaration forParameter= enhancedFor.getParameter();
						Type forParmType= forParameter.getType();
						ITypeBinding forParmTypeBinding= forParmType.resolveBinding();
						if (isStringBufferType(forParmTypeBinding)) {
							Expression forExpression= enhancedFor.getExpression();
							LocalVarChecker localVarChecker= new LocalVarChecker(forExpression);
							if (!localVarChecker.isValid()) {
								throw new AbortSearchException();
							}
						}
						return true;
					}

					@Override
					public boolean visit(ReturnStatement returnStatement) {
						Expression exp= returnStatement.getExpression();
						if (exp != null) {
							MethodDeclaration methodDeclaration=
									(MethodDeclaration)ASTNodes.getFirstAncestorOrNull(returnStatement, MethodDeclaration.class);
							if (methodDeclaration != null) {
								IMethodBinding returnStatementMethodBinding= methodDeclaration.resolveBinding();
								if (returnStatementMethodBinding != null) {
									ITypeBinding returnStatementTypeBinding= returnStatementMethodBinding.getReturnType();
									if (isStringBufferType(returnStatementTypeBinding)) {
										CheckNodeForValidReferences checkNode= new CheckNodeForValidReferences(exp, false);
										if (!checkNode.isValid()) {
											throw new AbortSearchException();
										}
									}
									return true;
								}
								throw new AbortSearchException();
							}

						}
						return true;
					}


					@Override
					public boolean visit(FieldAccess fieldAccess) {
						IVariableBinding fieldBinding= fieldAccess.resolveFieldBinding();
						if (fieldBinding == null) {
							throw new AbortSearchException();
						}
						return checkFieldAccess(fieldAccess, fieldBinding);
					}

					@Override
					public boolean visit(SuperFieldAccess superFieldAccess) {
						IVariableBinding fieldBinding= superFieldAccess.resolveFieldBinding();
						if (fieldBinding == null) {
							throw new AbortSearchException();
						}
						return checkFieldAccess(superFieldAccess, fieldBinding);
					}

					private boolean checkFieldAccess(ASTNode node, IVariableBinding fieldBinding) {
						ITypeBinding fieldTypeBinding= fieldBinding.getType();
						if (isStringBufferType(fieldTypeBinding)) {
							if (node.getLocationInParent() == MethodInvocation.EXPRESSION_PROPERTY) {
								MethodInvocation parent= (MethodInvocation)node.getParent();
								IMethodBinding fieldMethodBinding= parent.resolveMethodBinding();
								if (fieldMethodBinding != null) {
									ITypeBinding fieldMethodReturnTypeBinding= fieldMethodBinding.getReturnType();
									if (!isStringBufferType(fieldMethodReturnTypeBinding)) {
										return true;
									}
								}
							}
							Assignment assignment= (Assignment)ASTNodes.getFirstAncestorOrNull(node, Assignment.class);
							if (assignment != null) {
								Expression leftSide= assignment.getLeftHandSide();
								if (leftSide instanceof Name) {
									IBinding leftSideBinding= ((Name)leftSide).resolveBinding();
									if (leftSideBinding instanceof IVariableBinding) {
										IVariableBinding leftSideVarBinding= (IVariableBinding)leftSideBinding;
										ITypeBinding leftNameBindingType= leftSideVarBinding.getType();
										if (leftNameBindingType == null) {
											throw new AbortSearchException();
										}
										if (!isStringBufferType(leftNameBindingType)) {
											return true;
										}
										CheckNodeForValidReferences checkAssignmentValid= new CheckNodeForValidReferences(assignment, !leftSideVarBinding.isParameter());
										if (checkAssignmentValid.isValid()) {
											return true;
										}
									}
								} else {
									IVariableBinding leftFieldBinding= null;
									if (leftSide instanceof FieldAccess) {
										leftFieldBinding= ((FieldAccess)leftSide).resolveFieldBinding();
									} else if (leftSide instanceof SuperFieldAccess) {
										leftFieldBinding= ((SuperFieldAccess)leftSide).resolveFieldBinding();
									}
									if (leftFieldBinding == null) {
										throw new AbortSearchException();
									}
									ITypeBinding leftFieldBindingType= leftFieldBinding.getType();
									if (!isStringBufferType(leftFieldBindingType)) {
										return true;
									}
									CheckNodeForValidReferences checkAssignmentValid= new CheckNodeForValidReferences(assignment, false);
									if (checkAssignmentValid.isValid()) {
										return true;
									}
								}
							}
						}
						return true;
					}
				};
				try {
					visited.accept(checkMethodDeclaration);
					fMethodDeclarations.add(visited);
				} catch (AbortSearchException e) {
					// do nothing and fall-through
				}
			}
			return true;
		}
	}

	private static final class LocalVarChecker {
		private final ASTNode fNode;

		public LocalVarChecker(ASTNode node) {
			fNode= node;
		}

		public boolean isValid() {
			ASTVisitor visitor= new ASTVisitor() {
				@Override
				public boolean visit(final SimpleName visited) {
					IBinding binding= visited.resolveBinding();
					if (binding == null) {
						throw new AbortSearchException();
					}
					if (binding instanceof IVariableBinding) {
						IVariableBinding varBinding= (IVariableBinding)binding;
						if (varBinding.isField() || varBinding.isParameter() || varBinding.isRecordComponent()) {
							throw new AbortSearchException();
						}
					}
					return false;
				}
			};
			try {
				fNode.accept(visitor);
				return true;
			} catch (AbortSearchException e) {
				// fall-through
			}
			return false;
		}
	}

	private static final class LocalsOnlyStringBufferFinder extends ASTVisitor {

		private final StringBufferFinder fStringBufferFinder;

		public LocalsOnlyStringBufferFinder(List<CompilationUnitRewriteOperation> operations) {
			fStringBufferFinder= new StringBufferFinder(operations);
		}

		@Override
		public boolean visit(final VariableDeclarationStatement visited) {
			Type type= visited.getType();
			ITypeBinding typeBinding= type.resolveBinding();
			if (isStringBufferType(typeBinding)) {
				visited.accept(fStringBufferFinder);
				return false;
			}
			return true;
		}

		@Override
		public boolean visit(final ClassInstanceCreation visited) {
			Type type= visited.getType();
			ITypeBinding typeBinding= type.resolveBinding();
			if (isStringBufferType(typeBinding)) {
				Assignment assignment= (Assignment)ASTNodes.getFirstAncestorOrNull(visited, Assignment.class);
				if (assignment != null) {
					Expression leftSide= assignment.getLeftHandSide();
					if (leftSide instanceof Name) {
						IBinding leftSideBinding= ((Name)leftSide).resolveBinding();
						if (leftSideBinding instanceof IVariableBinding) {
							IVariableBinding leftSideVarBinding= (IVariableBinding)leftSideBinding;
							if (!leftSideVarBinding.isField() && !leftSideVarBinding.isParameter() && !leftSideVarBinding.isRecordComponent()) {
								visited.accept(fStringBufferFinder);
							}
						}
					}
				}
			}
			return true;
		}

		@Override
		public boolean visit(final EnhancedForStatement visited) {
			SingleVariableDeclaration forParameter= visited.getParameter();
			Type forParmType= forParameter.getType();
			ITypeBinding forParmTypeBinding= forParmType.resolveBinding();
			if (isStringBufferType(forParmTypeBinding)) {
				Expression forExpression= visited.getExpression();
				LocalVarChecker localVarChecker= new LocalVarChecker(forExpression);
				if (localVarChecker.isValid()) {
					forParameter.accept(fStringBufferFinder);
					forExpression.accept(fStringBufferFinder);
				}
			}
			return true;
		}
	}

	public static class ChangeStringBufferToStringBuilder extends CompilationUnitRewriteOperation {

		private final Type fType;
		private final SimpleName fName;

		public ChangeStringBufferToStringBuilder(final Type type) {
			this.fType= type;
			this.fName= null;
		}

		public ChangeStringBufferToStringBuilder(final SimpleName name) {
			this.fType= null;
			this.fName= name;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			ImportRewrite importRewrite= cuRewrite.getImportRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.StringBufferToStringBuilderCleanUp_description, cuRewrite);
			rewrite.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				@Override
				public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
					if (Boolean.TRUE.equals(nodeWithComment.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
						return new SourceRange(nodeWithComment.getStartPosition(), nodeWithComment.getLength());
					}

					return super.computeSourceRange(nodeWithComment);
				}
			});

			importRewrite.addImport(StringBuilder.class.getCanonicalName());
			if (fType != null) {
				rewrite.replace(fType, ast.newSimpleType(ast.newName("StringBuilder")), group); //$NON-NLS-1$
			} else {
				rewrite.replace(fName, ast.newSimpleName("StringBuilder"), group); //$NON-NLS-1$
			}
		}
	}

	public static ICleanUpFixCore createCleanUp(final CompilationUnit compilationUnit, boolean forLocalsOnly) {
		if (!JavaModelUtil.is50OrHigher(compilationUnit.getJavaElement().getJavaProject()))
			return null;

		List<CompilationUnitRewriteOperation> operations= new ArrayList<>();

		if (forLocalsOnly) {
			List<MethodDeclaration> validMethodDeclarations= new ArrayList<>();
			ValidLocalMethodFinder finder= new ValidLocalMethodFinder(validMethodDeclarations);
			compilationUnit.accept(finder);
			LocalsOnlyStringBufferFinder localsFinder= new LocalsOnlyStringBufferFinder(operations);
			for (MethodDeclaration methodDeclaration : validMethodDeclarations) {
				methodDeclaration.accept(localsFinder);
			}
		} else {
            StringBufferFinder finder= new StringBufferFinder(operations);
            compilationUnit.accept(finder);
		}

		if (operations.isEmpty()) {
			return null;
		}

		CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[0]);
		return new StringBufferToStringBuilderFixCore(FixMessages.StringBufferToStringBuilderFix_convert_msg, compilationUnit, ops);
	}

	protected StringBufferToStringBuilderFixCore(final String name, final CompilationUnit compilationUnit, final CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}

}
