/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.jdt.internal.corext.dom;

import java.util.ArrayList;

import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Statement;

import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;

public class ASTWithExistingFlattener extends ASTFlattener {

	private static final String KEY= "ExistingASTNode";

	public static class ExistingNodeMarker {
		public ASTNode existingNode;
		public int offset;
		public int length;
	}
	
	public static ASTNode getPlaceholder(ASTNode existingNode) {
		ASTNode placeHolder;
		if (existingNode instanceof Expression) {
			placeHolder= existingNode.getAST().newSimpleName("z");
		} else if (existingNode instanceof Statement) {
			if (existingNode.getNodeType() == ASTNode.BLOCK) {
				placeHolder= existingNode.getAST().newBlock();
			} else {
				placeHolder= existingNode.getAST().newReturnStatement();
			}
		} else if (existingNode instanceof BodyDeclaration) {
			placeHolder= existingNode.getAST().newInitializer();
		} else {
			return null;
		}
		
		ExistingNodeMarker marker= new ExistingNodeMarker();
		marker.existingNode= existingNode;
		marker.offset= -1;
		marker.length= 0;
		
		placeHolder.setProperty(KEY, marker);
		return placeHolder;
	}
	
	private static ExistingNodeMarker getMarker(ASTNode node) {
		return (ExistingNodeMarker) node.getProperty(KEY);
	} 
	
	private ArrayList fExistingNodes;

	public ASTWithExistingFlattener() {
		super();
		fExistingNodes= new ArrayList(10);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.dom.ASTFlattener#reset()
	 */
	public void reset() {
		super.reset();
		fExistingNodes.clear();
	}
	
	public ExistingNodeMarker[] getExistingNodeMarkers() {
		return (ExistingNodeMarker[]) fExistingNodes.toArray(new ExistingNodeMarker[fExistingNodes.size()]);
	}
	
	/**
	 * Returns the string accumulated in the visit formatted using the default formatter.
	 * Updates the existing node's positions.
	 *
	 * @return the serialized and formatted code.
	 */	
	public String getFormattedResult(int initialIndentationLevel, String lineDelimiter) {
		int tabWidth= CodeFormatterUtil.getTabWidth();
		ExistingNodeMarker[] markers= getExistingNodeMarkers();
		int nExistingNodes= markers.length;

		int[] positions= new int[nExistingNodes*2];
		for (int i= 0, k= 0; i < nExistingNodes; i++) {
			int startPos= markers[i].offset;
			int length= markers[i].length;
			positions[k++]= startPos;
			positions[k++]= startPos + length - 1;
		}		
		
		ICodeFormatter formatter= ToolFactory.createDefaultCodeFormatter(null);
		String formatted= formatter.format(getResult(), initialIndentationLevel, positions, lineDelimiter);
		
		for (int i= 0, k= 0; i < nExistingNodes; i++) {
			int startPos= positions[k++];
			int endPos= positions[k++] + 1;
			markers[i].offset= startPos;
			markers[i].length= endPos - startPos;
		}
		return formatted;
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#preVisit(ASTNode)
	 */
	public void preVisit(ASTNode node) {
		ExistingNodeMarker marker= getMarker(node);
		if (marker != null) {
			marker.offset= fResult.length();
			fExistingNodes.add(marker);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#postVisit(ASTNode)
	 */
	public void postVisit(ASTNode node) {
		ExistingNodeMarker marker= getMarker(node);
		if (marker != null) {
			marker.length= fResult.length() - marker.offset;
		}
	}
	
	
	
	public String generateFormatted(ASTNode node, TextBuffer existingSource, int initialIndentationLevel) {
		int tabWidth= CodeFormatterUtil.getTabWidth();
		String lineDelimiter= existingSource.getLineDelimiter();
		
		node.accept(this);
		String formatted= getFormattedResult(initialIndentationLevel, lineDelimiter);

		ExistingNodeMarker[] markers= getExistingNodeMarkers();
		
		StringBuffer buf= new StringBuffer();
		int currPos= 0;
		for (int i= 0; i < markers.length; i++) {
			int existingStartPos= markers[i].offset;
			ASTNode existingNode= markers[i].existingNode;
			
			buf.append(formatted.substring(currPos, existingStartPos));
			// createInsert(formatted.substring(currPos, existingStartPos), insertPos);
			
			// createMove(existingNode.getStartPosition(), existingNode.getLength(), insertPos);
			
			int nodeStartLine= existingSource.getLineOfOffset(existingNode.getStartPosition());
			int nodeIndent= existingSource.getLineIndent(nodeStartLine, tabWidth);
			String nodeContent= existingSource.getContent(existingNode.getStartPosition(), existingNode.getLength());
			
			String currLine= getCurrentLine(formatted, existingStartPos);
			int currIndent= Strings.computeIndent(currLine, tabWidth);
			
			if (nodeIndent != currIndent) {
				String[] lines= Strings.convertIntoLines(nodeContent);
				String indentString= Strings.getIndentString(currLine, tabWidth);
				for (int k= 0; k < lines.length; k++) {
					if (k > 0) {
						buf.append(lineDelimiter);
						buf.append(indentString); // no indent for first line (contained in the formatted string)
						buf.append(Strings.trimIndent(lines[k], nodeIndent, tabWidth));
					} else {
						buf.append(lines[k]);
					}
				}
			} else {
				buf.append(nodeContent);
			}
			currPos= existingStartPos + markers[i].length;
		}
		
		// createInsert(formatted.substring(currPos, formatted.length()), insertPos);
		buf.append(formatted.substring(currPos, formatted.length()));
	
		return buf.toString();
	}

	private String getCurrentLine(String str, int pos) {
		for (int i= pos - 1; i>= 0; i--) {
			char ch= str.charAt(i);
			if (ch == '\n' || ch == '\r') {
				return str.substring(i + 1, pos);
			}
		}
		return str.substring(0, pos);
	}
	
	


}
