/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.SimpleName;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;

class TypeReferenceFinder {

	public static ISourceRange[] findTypeReferenceRanges(SearchResult[] searchResults, ICompilationUnit cu) throws JavaModelException {
		Assert.isNotNull(searchResults);
		if (searchResults.length == 0)
			return new ISourceRange[0];
		
		TypeReferenceFinderVisitor visitor= new TypeReferenceFinderVisitor(searchResults);
		AST.parseCompilationUnit(cu, false).accept(visitor);
		return visitor.getFoundRanges();
	}
	
	private static class TypeReferenceFinderVisitor extends ASTVisitor{
		
		private Collection fFoundRanges;
		private SearchResult[] fSearchResults;
		
		TypeReferenceFinderVisitor(SearchResult[] searchResults){
			fSearchResults= searchResults;
			fFoundRanges= new ArrayList(0);
		}
		
		ISourceRange[] getFoundRanges(){
			return (ISourceRange[]) fFoundRanges.toArray(new ISourceRange[fFoundRanges.size()]);
		}
		
		private static boolean areReportedForSameNode(ASTNode node, SearchResult searchResult){
			if (node.getStartPosition() != searchResult.getStart())
				return false;
			if (ASTNodes.getExclusiveEnd(node) < searchResult.getEnd())	
				return false;
		
			return true;			
		}
		
		private boolean isReported(ASTNode node){
			for (int i= 0; i < fSearchResults.length; i++) {
				if (areReportedForSameNode(node, fSearchResults[i]))
					return true;
			}
			return false;
		}
		
		//--- visit methods ---

		public boolean visit(SimpleName node) {
			if (! isReported(node))
				return true;
				
			fFoundRanges.add(new SourceRange(node));				
			return false;	
		}

	}
}
