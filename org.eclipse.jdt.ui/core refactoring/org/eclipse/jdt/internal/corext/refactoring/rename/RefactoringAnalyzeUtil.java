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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import org.eclipse.jface.text.IRegion;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStringStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

public class RefactoringAnalyzeUtil {
	
	private RefactoringAnalyzeUtil(){
	}
	
	public static ICompilationUnit getWorkingCopyWithNewContent(TextEdit[] edits, TextChange change, ICompilationUnit cu) throws CoreException {
		for (int i= 0; i < edits.length; i++) {
			TextChangeCompatibility.addTextEdit(change, "", edits[i]); //$NON-NLS-1$
		}
		ICompilationUnit wc= WorkingCopyUtil.getNewWorkingCopy(cu);
		Assert.isTrue(! cu.equals(wc));
		wc.getBuffer().setContents(change.getPreviewContent());
		return wc;
	}

	public static IRegion[] getRanges(TextEdit[] edits, TextChange change){
		IRegion[] result= new IRegion[edits.length];
		for (int i= 0; i < edits.length; i++) {
			result[i]= RefactoringAnalyzeUtil.getTextRange(edits[i], change);
		}
		return result;
	}

	public static RefactoringStatus reportProblemNodes(String modifiedWorkingCopySource, SimpleName[] problemNodes) {
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < problemNodes.length; i++) {
			RefactoringStatusContext context= new JavaStringStatusContext(modifiedWorkingCopySource, new SourceRange(problemNodes[i]));
			result.addError(RefactoringCoreMessages.getFormattedString("RefactoringAnalyzeUtil.name_collision", problemNodes[i].getIdentifier()), context); //$NON-NLS-1$
		}
		return result;
	}

	public static TextEdit getFirstEdit(TextEdit[] edits){
		Arrays.sort(edits, new Comparator(){
			public int compare(Object o1, Object o2){
				return ((TextEdit)o1).getOffset() - ((TextEdit)o2).getOffset();
			}
		});
		return edits[0];
	}

	public static String getFullBindingKey(VariableDeclaration decl){
		StringBuffer buff= new StringBuffer();
		if (decl.resolveBinding() != null) {
			buff.append(decl.resolveBinding().getVariableId());
			buff.append('/');
		} else {
			buff.append(decl.getStartPosition()).append('*').append('/'); //ensure unique key if unresolvable
		}
		
		AnonymousClassDeclaration acd= (AnonymousClassDeclaration)ASTNodes.getParent(decl, AnonymousClassDeclaration.class);
		if (acd != null && acd.resolveBinding() != null){
			if (acd.resolveBinding().getKey() != null)
				buff.append(acd.resolveBinding().getKey());
			else
				buff.append("AnonymousClassDeclaration");	 //$NON-NLS-1$
			buff.append('/');	
		}	
		
		TypeDeclaration td= (TypeDeclaration)ASTNodes.getParent(decl, TypeDeclaration.class);
		if (td != null && td.resolveBinding() != null){
			if (td.resolveBinding().getKey() != null)
				buff.append(td.resolveBinding().getKey());
			else
				buff.append("TypeDeclaration");	 //$NON-NLS-1$
			buff.append('/');	
		}
		
		MethodDeclaration md= (MethodDeclaration)ASTNodes.getParent(decl, MethodDeclaration.class);
		if (md != null && md.resolveBinding() != null){
			if (md.resolveBinding().getKey() != null)
				buff.append(md.resolveBinding().getKey());
			else
				buff.append("MethodDeclaration");	 //$NON-NLS-1$
		}
		return buff.toString();
	}

	public static MethodDeclaration getMethodDeclaration(TextEdit edit, TextChange change, CompilationUnit cuNode){
		ASTNode decl= RefactoringAnalyzeUtil.getNameNode(RefactoringAnalyzeUtil.getTextRange(edit, change), cuNode);
		return ((MethodDeclaration)ASTNodes.getParent(decl, MethodDeclaration.class));
	}

	public static Block getBlock(TextEdit edit, TextChange change, CompilationUnit cuNode){
		ASTNode decl= RefactoringAnalyzeUtil.getNameNode(RefactoringAnalyzeUtil.getTextRange(edit, change), cuNode);
		return ((Block)ASTNodes.getParent(decl, Block.class));
	}
	
	public static IProblem[] getIntroducedCompileProblems(CompilationUnit newCUNode, CompilationUnit oldCuNode) {
		Set subResult= new HashSet();				
		Set oldProblems= getOldProblems(oldCuNode);
		IProblem[] newProblems= ASTNodes.getProblems(newCUNode, ASTNodes.INCLUDE_ALL_PARENTS, ASTNodes.PROBLEMS);
		for (int i= 0; i < newProblems.length; i++) {
			IProblem correspondingOld= findCorrespondingProblem(oldProblems, newProblems[i]);
			if (correspondingOld == null)
				subResult.add(newProblems[i]);
		}
		return (IProblem[]) subResult.toArray(new IProblem[subResult.size()]);
	}
	
	private static IProblem findCorrespondingProblem(Set oldProblems, IProblem iProblem) {
		for (Iterator iter= oldProblems.iterator(); iter.hasNext();) {
			IProblem oldProblem= (IProblem) iter.next();
			if (isCorresponding(oldProblem, iProblem))
				return oldProblem;
		}
		return null;
	}
	
	private static boolean isCorresponding(IProblem oldProblem, IProblem iProblem) {
		if (oldProblem.getID() != iProblem.getID())		
			return false;
		if (! oldProblem.getMessage().equals(iProblem.getMessage()))	
			return false;
		return true;
	}


	public static String getFullDeclarationBindingKey(TextEdit[] edits, CompilationUnit cuNode) {
		Name declarationNameNode= getNameNode(getTextRange(getFirstEdit(edits), null), cuNode);
		return getFullBindingKey((VariableDeclaration)declarationNameNode.getParent());
	}

	private static SimpleName getSimpleName(ASTNode node){
		if (node instanceof SimpleName)
			return (SimpleName)node;
		if (node instanceof VariableDeclaration)
			return ((VariableDeclaration)node).getName();
		return null;	
	}

	private static SimpleName getNameNode(IRegion range, CompilationUnit cuNode) {
		Selection sel= Selection.createFromStartLength(range.getOffset(), range.getLength());
		SelectionAnalyzer analyzer= new SelectionAnalyzer(sel, true);
		cuNode.accept(analyzer);
		return getSimpleName(analyzer.getFirstSelectedNode());
	}

	private static IRegion getTextRange(TextEdit edit, TextChange change){
		if (change == null)
			return edit.getRegion();
		 else
			return change.getNewTextRange(edit);
	}

	private static Set getOldProblems(CompilationUnit oldCuNode) {
		return new HashSet(Arrays.asList(ASTNodes.getProblems(oldCuNode, ASTNodes.INCLUDE_ALL_PARENTS, ASTNodes.PROBLEMS)));
	}
}
