package org.eclipse.jdt.internal.ui.text.correction.proposals;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import org.eclipse.jdt.internal.corext.fix.LinkedProposalModelCore;

public interface ILinkedCorrectionProposalCore {
	public ImportRewrite getImportRewrite();
	public void setImportRewrite(ImportRewrite rewrite);
	public ImportRewrite createImportRewrite(CompilationUnit astRoot);
	//public void addEdits(IDocument document, TextEdit editRoot) throws CoreException;

	public void addLinkedPosition(ITrackedNodePosition position, boolean isFirst, String groupId);
	public void addLinkedPosition(ITrackedNodePosition position, int sequenceRank, String groupID);
	public void addLinkedPositionProposal(String groupID, ITypeBinding type);
	public void addLinkedPositionProposal(String groupID, String proposal);
	public LinkedProposalModelCore getLinkedProposalModel();
	public void setEndPosition(ITrackedNodePosition position);
	public void setLinkedProposalModel(LinkedProposalModelCore model);
}
