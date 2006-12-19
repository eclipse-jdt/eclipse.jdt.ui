/**
 *
 **/
package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.Statement;

import org.eclipse.jdt.internal.corext.fix.LinkedFix.AbstractLinkedFixRewriteOperation;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

public abstract class ConvertLoopOperation extends AbstractLinkedFixRewriteOperation {
	
	private ForStatement fStatement;
	private ConvertLoopOperation fOperation;
	
	public ConvertLoopOperation(ForStatement statement) {
		fStatement= statement;
	}
	
	public void setBodyConverter(ConvertLoopOperation operation) {
		fOperation= operation;
	}
	
	public ForStatement getForStatement() {
		return fStatement;
	}
	
	protected abstract Statement convert(CompilationUnitRewrite cuRewrite, TextEditGroup group, LinkedProposalModel positionGroups) throws CoreException;
	
	protected Statement getBody(CompilationUnitRewrite cuRewrite, TextEditGroup group, LinkedProposalModel positionGroups) throws CoreException {
		if (fOperation != null) {
			return fOperation.convert(cuRewrite, group, positionGroups);
		} else {
    		return (Statement)cuRewrite.getASTRewrite().createMoveTarget(getForStatement().getBody());
    	}
    }
	
}