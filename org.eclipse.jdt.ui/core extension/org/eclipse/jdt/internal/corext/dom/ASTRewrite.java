package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.jdt.core.dom.ASTNode;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.CopySourceEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.MultiTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;

/**
  */
public class ASTRewrite {
	
	private static final String CHANGEKEY= "ASTChangeData";
	private static final String COPYSOURCEKEY= "ASTCopySource";
	
	private ASTNode fRootNode;
	
	private static final class ASTCopySource {
		public CopySourceEdit copySource;
	}	
		
	private static final class ASTReplace {
		public ASTNode replacingNode;
	}
	
	private static final class ASTModify {
		public ASTNode modifiedNode;
	}	
	
	public ASTRewrite(ASTNode node) {
		fRootNode= node;
	}
	
	
	public TextEdit rewriteNode(TextBuffer textBuffer) {
		MultiTextEdit rootEdit= new MultiTextEdit();
		ASTRewriteAnalyzer visitor= new ASTRewriteAnalyzer(textBuffer, rootEdit, this);
		fRootNode.accept(visitor);
		return rootEdit;
	}
	
	public final ASTNode createCopyTarget(ASTNode node) {
		Assert.isTrue(node.getProperty(COPYSOURCEKEY) == null, "Node used as more than one copy source");
		CopySourceEdit edit= new CopySourceEdit(node.getStartPosition(), node.getLength());
		node.setProperty(COPYSOURCEKEY, edit);
		return ASTWithExistingFlattener.getPlaceholder(node);
	}

	public final void markAsInserted(ASTNode node) {
		Assert.isTrue(node.getStartPosition() == -1, "Tries to mark existing node as inserted");
		node.setSourceRange(-1, 0);
	}
	
	public final void markAsRemoved(ASTNode node) {
		markAsReplaced(node, null);
	}
	
	public final void markAsReplaced(ASTNode node, ASTNode replacingNode) {
		ASTReplace replace= new ASTReplace();
		replace.replacingNode= replacingNode;
		node.setProperty(CHANGEKEY, replace);
	}
	
	public final void markAsModified(ASTNode node, ASTNode modifiedNode) {
		Assert.isTrue(node.getClass().equals(modifiedNode.getClass()), "Tries to modify with a node of different type");
		ASTModify modify= new ASTModify();
		modify.modifiedNode= modifiedNode;
		node.setProperty(CHANGEKEY, modify);
	}
	
	
	public final boolean isInserted(ASTNode node) {
		return node.getStartPosition() == -1;
	}
	
	public final boolean isReplaced(ASTNode node) {
		return node.getProperty(CHANGEKEY) instanceof ASTReplace;
	}
	
	public final boolean isRemoved(ASTNode node) {
		Object info= node.getProperty(CHANGEKEY);
		return info instanceof ASTReplace && (((ASTReplace) info).replacingNode == null);
	}	
	
	public final boolean isModified(ASTNode node) {
		return node.getProperty(CHANGEKEY) instanceof ASTModify;
	}	
	
	public final ASTNode getModifiedNode(ASTNode node) {
		Object info= node.getProperty(CHANGEKEY);
		if (info instanceof ASTModify) {
			return ((ASTModify) info).modifiedNode;
		}
		return null;
	}

	public final ASTNode getReplacingNode(ASTNode node) {
		Object info= node.getProperty(CHANGEKEY);
		if (info instanceof ASTReplace) {
			return ((ASTReplace) info).replacingNode;
		}
		return null;
	}
	
	public final CopySourceEdit getCopySourceEdit(ASTNode node) {
		return (CopySourceEdit) node.getProperty(COPYSOURCEKEY);
	}
	

}
