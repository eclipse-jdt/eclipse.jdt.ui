package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.dom.NodeFinder;

/**
  */
public class CorrectionContext implements ICorrectionContext {
	
	private ICompilationUnit fCompilationUnit;
	private int fOffset;
	private int fLength;
	
	private int fProblemId;
	private String[] fProblemArguments;
	
	private CompilationUnit fASTRoot;
	private NodeFinder fNodeFinder;
	
	/**
	 * Constructor for CorrectionContext.
	 */
	public CorrectionContext(ICompilationUnit cu) {
		this(cu, -1, 0, 0, null);
	}

	public CorrectionContext(ICompilationUnit cu, int offset, int length, int problemId, String[] problemArguments) {
		fCompilationUnit= cu;
		fASTRoot= null;
		fNodeFinder= null;		
		initialize(offset, length, problemId, problemArguments);
	}		
	
	
	public void initialize(int offset, int length, int problemId, String[] problemArguments) {
		fOffset= offset;
		fLength= length;
		fProblemId= problemId;
		fProblemArguments= problemArguments;
		fNodeFinder= null;
	}
		
		
	public int getProblemId() {
		return fProblemId;
	}
	
	public String[] getProblemArguments() {
		return fProblemArguments;
	}

	/**
	 * Returns the compilation unit.
	 * @return Returns a ICompilationUnit
	 */
	public ICompilationUnit getCompilationUnit() {
		return fCompilationUnit;
	}
	
	/**
	 * Returns the length.
	 * @return int
	 */
	public int getLength() {
		return fLength;
	}

	/**
	 * Returns the offset.
	 * @return int
	 */
	public int getOffset() {
		return fOffset;
	}
	
	public CompilationUnit getASTRoot() {
		if (fASTRoot == null) {
			fASTRoot= AST.parseCompilationUnit(fCompilationUnit, true);
		}
		return fASTRoot;
	}

	public NodeFinder getNodeFinder() {
		if (fNodeFinder == null) {
			fNodeFinder= new NodeFinder(fOffset, fLength);
			getASTRoot().accept(fNodeFinder);
		}
		return fNodeFinder;	
	}
	
	public ASTNode getCoveringNode() {
		return getNodeFinder().getCoveringNode();
	}
	
	public ASTNode getCoveredNode() {
		return getNodeFinder().getCoveredNode();
	}	
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof CorrectionContext) {
			CorrectionContext other= (CorrectionContext) obj;
			return fCompilationUnit.equals(other.fCompilationUnit)
				&& fOffset == other.fOffset && fLength == other.fLength
				&& fProblemId == other.fProblemId;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return fCompilationUnit.hashCode() + (fOffset << 4) + fLength;
	}

}
