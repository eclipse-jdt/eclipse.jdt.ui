/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.textmanipulation.NopTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;

public class SourceContext {

	private TextBuffer fBuffer;
	private MethodDeclaration fDeclaration;
	private ASTRewrite fRewriter;
	private SourceAnalyzer fAnalyzer;

	public SourceContext(ICompilationUnit unit, MethodDeclaration declaration) throws JavaModelException {
		super();
		fBuffer= TextBuffer.create(unit.getBuffer().getContents());
		fDeclaration= declaration;
		List parameters= fDeclaration.parameters();
		for (Iterator iter= parameters.iterator(); iter.hasNext();) {
			SingleVariableDeclaration element= (SingleVariableDeclaration)iter.next();
			ParameterData data= new ParameterData(element);
			element.setProperty(ParameterData.PROPERTY, data);
		}
		fRewriter= new ASTRewrite(fDeclaration);
		fAnalyzer= new SourceAnalyzer(fDeclaration);
	}

	public RefactoringStatus checkActivation() {
		return fAnalyzer.checkActivation();
	}
	
	public void initialize() {
		fAnalyzer.analyzeParameters();
	}

	public int getNumberOfStatements() {
		return fDeclaration.getBody().statements().size();
	}
	
	public ParameterData getParameterData(int index) {
		SingleVariableDeclaration decl= (SingleVariableDeclaration)fDeclaration.parameters().get(index);
		return (ParameterData)decl.getProperty(ParameterData.PROPERTY);
	}
	
	public ASTNode[] getInlineNodes(String[] expressions, ASTRewrite targetFactory, List usedCallerNames) throws CoreException {
		List result= new ArrayList(1);
		
		replaceParameterWithExpression(expressions);
		makeNamesUnique(usedCallerNames);
		
		TextRange range= getTextRange();
		NopTextEdit root= new NopTextEdit(range);
		fRewriter.rewriteNode(fBuffer, root, null);
		TextBufferEditor editor= new TextBufferEditor(fBuffer);
		editor.add(root);
		editor.performEdits(null);
		fRewriter.removeModifications();
		
		String content= fBuffer.getContent(range.getOffset(), range.getLength());
		String lines[]= Strings.convertIntoLines(content);
		Strings.trimIndentation(lines, CodeFormatterUtil.getTabWidth(), false);
		result.add(targetFactory.createPlaceholder(Strings.concatenate(lines, fBuffer.getLineDelimiter()), ASTRewrite.STATEMENT));
		return (ASTNode[]) result.toArray(new ASTNode[result.size()]);
	}

	public void replaceParameterWithExpression(String[] expressions) {
		for (int i= 0; i < expressions.length; i++) {
			String expression= expressions[i];
			ParameterData parameter= getParameterData(i);
			List references= parameter.references();
			for (Iterator iter= references.iterator(); iter.hasNext();) {
				ASTNode element= (ASTNode) iter.next();
				ASTNode newNode= fRewriter.createPlaceholder(expression, ASTRewrite.getPlaceholderType(element));
				fRewriter.markAsReplaced(element, newNode);
			}
		}
	}

	private void makeNamesUnique(List usedCallerNames) {
		Collection usedCalleeNames= fAnalyzer.getUsedNames();
		for (Iterator iter= usedCalleeNames.iterator(); iter.hasNext();) {
			SourceAnalyzer.NameData nd= (SourceAnalyzer.NameData) iter.next();
			if (usedCallerNames.contains(nd.getName())) {
				String newName= proposeName(usedCallerNames, nd.getName());
				List references= nd.references();
				for (Iterator refs= references.iterator(); refs.hasNext();) {
					SimpleName element= (SimpleName) refs.next();
					ASTNode newNode= fRewriter.createPlaceholder(newName, ASTRewrite.EXPRESSION);
					fRewriter.markAsReplaced(element, newNode);
				}
			}
		}
	}

	private String proposeName(List used, String start) {
		int i= 1;
		String result= start + i++;
		while (used.contains(result)) {
			result= start + i++;
		}
		return result;
	}

	private TextRange getTextRange() {
		List statements= fDeclaration.getBody().statements();
		if (statements.isEmpty())
			return null;
		int start= ((ASTNode)statements.get(0)).getStartPosition();
		ASTNode last= (ASTNode)statements.get(statements.size() - 1);
		int length = last.getStartPosition() - start + last.getLength();
		return TextRange.createFromStartAndLength(start, length);
	}
}
