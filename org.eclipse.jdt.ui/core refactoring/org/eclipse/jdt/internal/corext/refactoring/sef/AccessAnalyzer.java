/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.sef;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.BindingIdentifier;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;

class AccessAnalyzer extends ASTVisitor {

	private ICompilationUnit fCUnit;
	private BindingIdentifier fFieldIdentifier;
	private BindingIdentifier fDeclaringClassIdentifier;	
	private String fGetter;
	private String fSetter;
	private TextChange fChange;
	private RefactoringStatus fStatus;
	private boolean fSetterMustReturnValue;
	private boolean fEncapsulateDeclaringClass;
	private boolean fIsFieldFinal;

	private static final String READ_ACCESS= RefactoringCoreMessages.getString("SelfEncapsulateField.AccessAnalyzer.encapsulate_read_access"); //$NON-NLS-1$
	private static final String WRITE_ACCESS= RefactoringCoreMessages.getString("SelfEncapsulateField.AccessAnalyzer.encapsulate_write_access"); //$NON-NLS-1$
	private static final String PREFIX_ACCESS= RefactoringCoreMessages.getString("SelfEncapsulateField.AccessAnalyzer.encapsulate_prefix_access"); //$NON-NLS-1$
	private static final String POSTFIX_ACCESS= RefactoringCoreMessages.getString("SelfEncapsulateField.AccessAnalyzer.encapsulate_postfix_access"); //$NON-NLS-1$
		
	public AccessAnalyzer(SelfEncapsulateFieldRefactoring refactoring, ICompilationUnit unit, BindingIdentifier field, BindingIdentifier declaringClass, TextChange change) {
		Assert.isNotNull(refactoring);
		Assert.isNotNull(unit);
		Assert.isNotNull(field);
		Assert.isNotNull(declaringClass);
		Assert.isNotNull(change);
		fCUnit= unit;
		fFieldIdentifier= field;
		fDeclaringClassIdentifier= declaringClass;
		fChange= change;
		fGetter= refactoring.getGetterName();
		fSetter= refactoring.getSetterName();
		fEncapsulateDeclaringClass= refactoring.getEncapsulateDeclaringClass();
		try {
			fIsFieldFinal= Flags.isFinal(refactoring.getField().getFlags());
		} catch (JavaModelException e) {
			// assume non final field
		}
		fStatus= new RefactoringStatus();
	}

	public boolean getSetterMustReturnValue() {
		return fSetterMustReturnValue;
	}

	public RefactoringStatus getStatus() {
		return fStatus;
	}
	
	public boolean visit(Assignment node) {
		Expression lhs= node.getLeftHandSide();
		if (!considerBinding(resolveBinding(lhs), lhs))
			return true;
			
		checkParent(node);
		if (!fIsFieldFinal)
			fChange.addTextEdit(WRITE_ACCESS, new EncapsulateWriteAccess(fGetter, fSetter, node));
		node.getRightHandSide().accept(this);
		return false;
	}

	public boolean visit(SimpleName node) {
		if (!node.isDeclaration() && considerBinding(node.resolveBinding(), node))
			fChange.addTextEdit(READ_ACCESS, new EncapsulateReadAccess(fGetter, node));
		return true;
	}
	
	public boolean visit(PrefixExpression node) {
		Expression operand= node.getOperand();
		if (!considerBinding(resolveBinding(operand), operand))
			return true;
		
		PrefixExpression.Operator operator= node.getOperator();	
		if (operator != PrefixExpression.Operator.INCREMENT && operator != PrefixExpression.Operator.DECREMENT)
			return true;
			
		checkParent(node);
		fChange.addTextEdit(PREFIX_ACCESS, new EncapsulatePrefixAccess(fGetter, fSetter, node));
		return false;
	}
	
	public boolean visit(PostfixExpression node) {
		Expression operand= node.getOperand();
		if (!considerBinding(resolveBinding(operand), operand))
			return true;

		ASTNode parent= node.getParent();
		if (!(parent instanceof ExpressionStatement)) {
			fStatus.addError(RefactoringCoreMessages.getString("SelfEncapsulateField.AccessAnalyzer.cannot_convert_postfix_expression"),  //$NON-NLS-1$
				JavaSourceContext.create(fCUnit, new SourceRange(node)));
			return false;
		}
		fChange.addTextEdit(POSTFIX_ACCESS, new EncapsulatePostfixAccess(fGetter, fSetter, node));
		return false;
	}
	
	private boolean considerBinding(IBinding binding, ASTNode node) {
		boolean result= fFieldIdentifier.matches(binding);
		if (!result || fEncapsulateDeclaringClass)
			return result;
			
		if (binding instanceof IVariableBinding) {
			TypeDeclaration type= (TypeDeclaration)ASTNodes.getParent(node, TypeDeclaration.class);
			if (type != null) {
				ITypeBinding declaringType= type.resolveBinding();
				return !fDeclaringClassIdentifier.matches(declaringType);
			}
		}
		return true;
	}
	
	private void checkParent(ASTNode node) {
		ASTNode parent= node.getParent();
		if (!(parent instanceof ExpressionStatement))
			fSetterMustReturnValue= true;
	}
		
	private IBinding resolveBinding(Expression expression) {
		if (expression instanceof SimpleName)
			return ((SimpleName)expression).resolveBinding();
		else if (expression instanceof QualifiedName)
			return ((QualifiedName)expression).resolveBinding();
		else if (expression instanceof FieldAccess)
			return ((FieldAccess)expression).getName().resolveBinding();
		return null;
	}	
}

