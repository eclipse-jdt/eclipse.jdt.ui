/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.sef;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.QualifiedName;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.textmanipulation.MultiTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;

final class EncapsulateWriteAccess extends MultiTextEdit {
	
	public EncapsulateWriteAccess(String getter, String setter, Assignment  assignment) {
		int offset= getOffset(assignment);
		int length= getLength(assignment, offset);
		int bracket= getBracketOffset(assignment);
		if (assignment.getOperator() == Assignment.Operator.ASSIGN) {
			add(SimpleTextEdit.createReplace(offset, length, setter + "(")); //$NON-NLS-1$
			add(SimpleTextEdit.createInsert(bracket, ")")); //$NON-NLS-1$
		} else {
			boolean needsParentheses= ASTNodes.needsParenthesis(assignment.getRightHandSide());
			add(SimpleTextEdit.createInsert(offset, setter + "(")); //$NON-NLS-1$
			add(SimpleTextEdit.createReplace(offset, length, getGetter(getter, assignment) + "() " + getOperator(assignment) + " " + (needsParentheses ? "(" : ""))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			add(SimpleTextEdit.createInsert(bracket, ")" + (needsParentheses ? ")" : ""))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}
	
	private static int getOffset(Assignment assignment) {
		Expression lhs= assignment.getLeftHandSide();
		if (lhs instanceof QualifiedName) {
			return ((QualifiedName)lhs).getName().getStartPosition();
		} else {
			return lhs.getStartPosition();
		}
	}
	
	private static int getLength(Assignment assignment, int offset) {
		return assignment.getRightHandSide().getStartPosition() - offset;
	}
	
	private static int getBracketOffset(Assignment assignment) {
		Expression expression= assignment.getRightHandSide();
		return expression.getStartPosition() + expression.getLength();
	}
	
	private static String getGetter(String getter, Assignment assignment) {
		Expression lhs= assignment.getLeftHandSide();
		if (lhs instanceof QualifiedName) {
			return ASTNodes.asString(((QualifiedName)lhs).getQualifier()) + "." + getter; //$NON-NLS-1$
		} else {
			return getter;
		}		
	}
	
	private static String getOperator(Assignment assignment) {
		String operator= assignment.getOperator().toString();
		return operator.substring(0, operator.length() - 1);
	}
}