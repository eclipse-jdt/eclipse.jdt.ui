package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Message;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.Context;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.base.StringContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

public class RefactoringAnalyzeUtil {
	
	private RefactoringAnalyzeUtil(){
	}
	
	public static ICompilationUnit getWorkingCopyWithNewContent(TextEdit[] edits, TextChange change, ICompilationUnit cu) throws JavaModelException {
		for (int i= 0; i < edits.length; i++) {
			change.addTextEdit("", edits[i]); //$NON-NLS-1$
		}
		ICompilationUnit wc= WorkingCopyUtil.getNewWorkingCopy(cu);
		Assert.isTrue(! cu.equals(wc));
		wc.getBuffer().setContents(change.getPreviewTextBuffer().getContent());
		return wc;
	}

	public static TextRange[] getRanges(TextEdit[] edits, TextChange change){
		TextRange[] result= new TextRange[edits.length];
		for (int i= 0; i < edits.length; i++) {
			result[i]= RefactoringAnalyzeUtil.getTextRange(edits[i], change);
		}
		return result;
	}

	public static RefactoringStatus reportProblemNodes(String modifiedWorkingCopySource, SimpleName[] problemNodes){
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < problemNodes.length; i++) {
			Context context= new StringContext(modifiedWorkingCopySource, new SourceRange(problemNodes[i]));
			result.addError(RefactoringCoreMessages.getString("RefactoringAnalyzeUtil.name_collision") + problemNodes[i].getIdentifier(), context); //$NON-NLS-1$
		}
		return result;
	}

	public static TextEdit getFirstEdit(TextEdit[] edits){
		Arrays.sort(edits, new Comparator(){
			public int compare(Object o1, Object o2){
				return ((TextEdit)o1).getTextRange().getOffset() - ((TextEdit)o2).getTextRange().getOffset();
			}
		});
		return edits[0];
	}

	public static String getFullBindingKey(VariableDeclaration decl){
		StringBuffer buff= new StringBuffer();
		buff.append(decl.resolveBinding().getVariableId());
		buff.append('/');
		
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

	public static RefactoringStatus analyzeIntroducedCompileErrors(String wcSource, CompilationUnit newCUNode, CompilationUnit oldCuNode) {
		RefactoringStatus subResult= new RefactoringStatus();				
		Set oldErrorMessages= getOldErrorMessages(oldCuNode);
		Message[] newErrorMessages= ASTNodes.getMessages(newCUNode, ASTNodes.INCLUDE_ALL_PARENTS);
		for (int i= 0; i < newErrorMessages.length; i++) {
			if (! oldErrorMessages.contains(newErrorMessages[i].getMessage())){
				Context context= new StringContext(wcSource, new SourceRange(newErrorMessages[i].getStartPosition(), newErrorMessages[i].getLength()));
				subResult.addError(newErrorMessages[i].getMessage(), context);
			}	
		}
		return subResult;
	}
	
	public static IProblem[] getIntroducedCompileProblems(String wcSource, CompilationUnit newCUNode, CompilationUnit oldCuNode) {
		Set subResult= new HashSet();				
		Set oldProblems= getOldProblems(oldCuNode);
		IProblem[] newProblems= ASTNodes.getProblems(newCUNode, ASTNodes.INCLUDE_ALL_PARENTS);
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
		if (oldProblem.getMessage().equals(iProblem.getMessage()))	
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

	private static SimpleName getNameNode(TextRange range, CompilationUnit cuNode) {
		Selection sel= Selection.createFromStartLength(range.getOffset(), range.getLength());
		SelectionAnalyzer analyzer= new SelectionAnalyzer(sel, true);
		cuNode.accept(analyzer);
		return getSimpleName(analyzer.getFirstSelectedNode());
	}

	private static TextRange getTextRange(TextEdit edit, TextChange change){
		if (change == null)
			return edit.getTextRange();
		 else
			return change.getNewTextRange(edit);
	}

	private static Set getOldProblems(CompilationUnit oldCuNode) {
		return new HashSet(Arrays.asList(ASTNodes.getProblems(oldCuNode, ASTNodes.INCLUDE_ALL_PARENTS)));
	}

	private static Set getOldErrorMessages(CompilationUnit cuNode) {
		Message[] oldMessages= ASTNodes.getMessages(cuNode, ASTNodes.INCLUDE_ALL_PARENTS);
		Set messageSet= new HashSet(oldMessages.length);
		for (int i= 0; i < oldMessages.length; i++) {
			messageSet.add(oldMessages[i].getMessage());
		}
		return messageSet;
	}	
}
