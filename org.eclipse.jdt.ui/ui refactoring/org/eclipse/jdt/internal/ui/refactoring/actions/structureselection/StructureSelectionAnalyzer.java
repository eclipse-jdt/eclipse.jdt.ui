package org.eclipse.jdt.internal.ui.refactoring.actions.structureselection;

import org.eclipse.jdt.core.IBuffer;

import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.util.SelectionAnalyzer;

public class StructureSelectionAnalyzer extends SelectionAnalyzer {

	private AstNode fLastCoveringNode;

	public StructureSelectionAnalyzer(IBuffer buffer, int start, int length) {
		super(buffer, start, length);
	}

	//overridden
	protected void handleSelectionCoveredByNode(AstNode node){
		fLastCoveringNode= node;
	}
	
	//overridden
	protected boolean isOkToIncludeExtraCharactersAtBeginning(){
		return true;
	}
	
	//overridden
	protected boolean isOkToIncludeExtraCharactersAtEnd(){
		return true;
	}

	public AstNode getLastCoveringNode() {
		return fLastCoveringNode;
	}
}