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
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;

public class TempOccurrenceFinder {

	//no instances
	private TempOccurrenceFinder(){}
	
	public static Integer[] findTempOccurrenceOffsets(VariableDeclaration temp, boolean includeReferences, boolean includeDeclaration) {
		TempOccurrenceAnalyzer analyzer= new TempOccurrenceAnalyzer(temp, includeReferences, includeDeclaration);
		getCompilationUnitNode(temp).accept(analyzer);
		return analyzer.getOffsets();
	} 
	
	public static ASTNode[] findTempOccurrenceNodes(VariableDeclaration temp, boolean includeReferences, boolean includeDeclaration) {
		TempOccurrenceAnalyzer analyzer= new TempOccurrenceAnalyzer(temp, includeReferences, includeDeclaration);
		getCompilationUnitNode(temp).accept(analyzer);
		return analyzer.getNodes();
	} 
	
	private static CompilationUnit getCompilationUnitNode(ASTNode node) {
		return (CompilationUnit)ASTNodes.getParent(node, CompilationUnit.class);
	}
	
	private static class TempOccurrenceAnalyzer extends ASTVisitor {
		private Set fNodes;
		private boolean fIncludeReferences;
		private boolean fIncludeDeclaration;
		private VariableDeclaration fTempDeclaration;
		private IBinding fTempBinding;
		
		TempOccurrenceAnalyzer(VariableDeclaration tempDeclaration, boolean includeReferences, boolean includeDeclaration){
			Assert.isNotNull(tempDeclaration);
			fNodes= new HashSet();
			fIncludeDeclaration= includeDeclaration;
			fIncludeReferences= includeReferences;
			fTempDeclaration= tempDeclaration;
			fTempBinding= tempDeclaration.resolveBinding();
		}
		
		Integer[] getOffsets(){
			List offsets= new ArrayList(fNodes.size());
			for (Iterator iter= fNodes.iterator(); iter.hasNext();) {
				ASTNode node= (ASTNode) iter.next();
				offsets.add(new Integer(node.getStartPosition()));
			}
			return (Integer[]) offsets.toArray(new Integer[offsets.size()]);
		}
		
		ASTNode[] getNodes(){
			return (ASTNode[]) fNodes.toArray(new ASTNode[fNodes.size()]);
		}
		
				
		private boolean visitNameReference(Name nameReference){
			if (nameReference.getParent() instanceof VariableDeclaration){
				if (((VariableDeclaration)nameReference.getParent()).getName() == nameReference)
					return true;
			}
			
			if (fIncludeReferences && fTempBinding != null && fTempBinding == nameReference.resolveBinding())
				fNodes.add(nameReference);
					
			return true;
		}

		private boolean visitVariableDeclaration(VariableDeclaration localDeclaration) {
			if (fIncludeDeclaration && fTempDeclaration.equals(localDeclaration))
				fNodes.add(localDeclaration.getName());
			
			return true;
		}
	
		//------- visit ------
		public boolean visit(SingleVariableDeclaration localDeclaration){
			return visitVariableDeclaration(localDeclaration);
		}
		
		public boolean visit(VariableDeclarationFragment localDeclaration){
			return visitVariableDeclaration(localDeclaration);	
		}
			
		public boolean visit(SimpleName singleNameReference){
			return visitNameReference(singleNameReference);
		}
	}
}
