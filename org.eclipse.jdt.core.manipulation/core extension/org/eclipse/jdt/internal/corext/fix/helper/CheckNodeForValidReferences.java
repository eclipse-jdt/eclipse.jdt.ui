package org.eclipse.jdt.internal.corext.fix.helper;

import java.util.Iterator;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.Type;

import org.eclipse.jdt.internal.corext.dom.AbortSearchException;

class CheckNodeForValidReferences {
	private static final String ITERATOR_NAME= Iterator.class.getCanonicalName();

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
					if (AbstractTool.isOfType(methodTypeBinding, ITERATOR_NAME)) {
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
					if (AbstractTool.isOfType(methodTypeBinding, ITERATOR_NAME)) {
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
					if (AbstractTool.isOfType(methodTypeBinding, ITERATOR_NAME)) {
						Expression exp= methodInvocation.getExpression();
						if (exp instanceof SimpleName) {
							IBinding binding= ((SimpleName)exp).resolveBinding();
							if (binding instanceof IVariableBinding &&
									!((IVariableBinding)binding).isField() &&
									!((IVariableBinding)binding).isParameter() &&
									!((IVariableBinding)binding).isRecordComponent()) {
								ITypeBinding typeBinding= ((IVariableBinding)binding).getType();
								if (AbstractTool.isOfType(typeBinding, ITERATOR_NAME)) {
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
				if (AbstractTool.isOfType(typeBinding, ITERATOR_NAME)) {
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
					if (AbstractTool.isOfType(simpleNameTypeBinding,ITERATOR_NAME)) {
						if (simpleName.getLocationInParent() == MethodInvocation.EXPRESSION_PROPERTY) {
							MethodInvocation methodInvocation= (MethodInvocation)simpleName.getParent();
							IMethodBinding methodInvocationBinding= methodInvocation.resolveMethodBinding();
							if (methodInvocationBinding == null) {
								throw new AbortSearchException();
							}
							ITypeBinding methodInvocationReturnType= methodInvocationBinding.getReturnType();
							if (!AbstractTool.isOfType(methodInvocationReturnType,ITERATOR_NAME)) {
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