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

package org.eclipse.jdt.internal.ui.text.correction;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.MemberEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextUtil;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class NewVariableCompletionProposal extends CUCorrectionProposal {
	
	private static class AddLocalVariableEdit extends SimpleTextEdit {
		private String fContent;
		private ASTNode fAstRoot;
		private int fTabSize;

		public AddLocalVariableEdit(ASTNode astRoot, String content, int tabSize) {
			fAstRoot= astRoot;
			fContent= content;
			fTabSize= tabSize;
		}
		
		/* non Java-doc
		 * @see TextEdit#getCopy
		 */
		public TextEdit copy() {
			return new AddLocalVariableEdit(fAstRoot, fContent, fTabSize);
		}
		
		/* non Java-doc
		 * @see TextEdit#connect
		 */
		public void connect(TextBufferEditor editor) throws CoreException {
			if (fAstRoot == null) {
				return;
			}
			
			TextBuffer buffer= editor.getTextBuffer();
			int offset= 0;
			String insertString= null;
			
			ASTNode curr= fAstRoot;
			while (curr != null && !(curr instanceof Block)) {
				curr= curr.getParent();
			}
			if (curr != null) {
				Block block= (Block) curr;
				List statements= block.statements();
				if (!statements.isEmpty()) {
					ASTNode statement= (ASTNode) statements.get(0);
					if (statement instanceof SuperConstructorInvocation || statement instanceof ConstructorInvocation ) {
						if (!fAstRoot.equals(statement) && statements.size() > 1) {
							statement= (ASTNode) statements.get(1);
						}
					}	
					offset= statement.getStartPosition();
					int startLine= buffer.getLineOfOffset(offset);
					String indentString= TextUtil.createIndentString(buffer.getLineIndent(startLine, fTabSize));
					String lineDelim= buffer.getLineDelimiter();
					insertString= StubUtility.codeFormat(fContent, 0, lineDelim) +  indentString;
				}
			}
			setTextRange(new TextRange(offset, 0));
			setText(insertString);
			super.connect(editor);
		}	
	}
	
	private static class AddParameterEdit extends SimpleTextEdit {
		private String fContent;
		private ASTNode fAstRoot;

		public AddParameterEdit(ASTNode astRoot, String content) {
			fAstRoot= astRoot;
			fContent= content;
		}
		
		/* non Java-doc
		 * @see TextEdit#getCopy
		 */
		public TextEdit copy() {
			return new AddParameterEdit(fAstRoot, fContent);
		}
		
		/* non Java-doc
		 * @see TextEdit#connect
		 */
		public void connect(TextBufferEditor editor) throws CoreException {
			if (fAstRoot == null) {
				return;
			}
			
			TextBuffer buffer= editor.getTextBuffer();
			int offset= 0;
			String insertString= "";
			
			ASTNode curr= fAstRoot;
			while (curr != null && !(curr instanceof MethodDeclaration)) {
				curr= curr.getParent();
			}
			if (curr != null) {
				MethodDeclaration declaration= (MethodDeclaration) curr;
				List params= declaration.parameters();
				if (!params.isEmpty()) {
					ASTNode p= (ASTNode) params.get(params.size() - 1);
					offset= p.getStartPosition() + p.getLength();
					insertString= ", " + fContent;
				} else {
					SimpleName name= declaration.getName();
					offset= name.getStartPosition() + name.getLength() - 1;
					insertString= fContent;
				}
			}
			setTextRange(new TextRange(offset, 0));
			setText(insertString);
			super.connect(editor);
		}	
	}	

	public static final int LOCAL= 1;
	public static final int FIELD= 2;
	public static final int PARAM= 3;

	private int  fVariableKind;
	private String fVariableName;
	private IMember fParentMember;

	public NewVariableCompletionProposal(IMember parentMember, ProblemPosition problemPos, String label, int variableKind, String variableName, int relevance) throws CoreException {
		super(label, problemPos, relevance);
		
		fVariableKind= variableKind;
		fVariableName= variableName;
		fParentMember= parentMember;
	}

	/*
	 * @see JavaCorrectionProposal#addEdits(CompilationUnitChange)
	 */
	protected void addEdits(CompilationUnitChange changeElement) throws CoreException {
		ProblemPosition problemPos= getProblemPosition();
		
		ICompilationUnit cu= getCompilationUnit();
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);

		ASTNode selectedNode= ASTResolving.findSelectedNode(astRoot, problemPos.getOffset(), problemPos.getLength());
		
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		ImportEdit importEdit= new ImportEdit(cu, settings);

		String content= generateStub(importEdit, selectedNode);
		if (!importEdit.isEmpty()) {
			changeElement.addTextEdit("Add imports", importEdit); //$NON-NLS-1$
		}		

		if (fVariableKind == LOCAL) {
			// new local variable
			changeElement.addTextEdit("Add local", new AddLocalVariableEdit(selectedNode, content, settings.tabWidth)); //$NON-NLS-1$
		} else if (fVariableKind == FIELD) {
			// new field
			changeElement.addTextEdit("Add field", createFieldEdit(content, settings.tabWidth)); //$NON-NLS-1$
		} else if (fVariableKind == PARAM) {
			// new parameter
			changeElement.addTextEdit("Add parameter", new AddParameterEdit(selectedNode, content)); //$NON-NLS-1$
		}
	}
	
	private TextEdit createFieldEdit(String content, int tabWidth) throws CoreException {
		IType type= (fParentMember.getElementType() ==  IJavaElement.TYPE) ? (IType) fParentMember : fParentMember.getDeclaringType();

		int insertPos= MemberEdit.ADD_AT_BEGINNING;
		IJavaElement anchor= type;

		IField[] field= type.getFields();
		if (field.length > 0) {
			anchor= field[field.length - 1];
			insertPos= MemberEdit.INSERT_AFTER;
		}

		MemberEdit memberEdit= new MemberEdit(anchor, insertPos, new String[] { content }, tabWidth);
		memberEdit.setUseFormatter(true);
		
		return memberEdit;
	}	

	
	private String generateStub(ImportEdit importEdit, ASTNode selectedNode) {
		StringBuffer buf= new StringBuffer();
		ITypeBinding varType= evaluateVariableType(importEdit, selectedNode);
		
		if (fVariableKind == FIELD) {
			buf.append("private "); //$NON-NLS-1$
		}
		buf.append(varType != null ? varType.getName() : "Object");
		buf.append(' ');
		buf.append(fVariableName);
		if (fVariableKind == LOCAL) {
			if (varType == null || !varType.isPrimitive()) {
				buf.append("= null");
			} else if (varType.getName().equals("boolean")) {
				buf.append("= false");
			} else {
				buf.append("= 0");
			}
		}
		if (fVariableKind != PARAM) {
			buf.append(";"); //$NON-NLS-1$
		}
			
		return buf.toString();
	}
		
	/*
	 * @see ICompletionProposal#getImage()
	 */
	public Image getImage() {
		return JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC);
	}
	
	private ITypeBinding evaluateVariableType(ImportEdit importEdit, ASTNode selectedNode) {
		if (selectedNode != null) {
			ITypeBinding binding= ASTResolving.getTypeBinding(selectedNode);
			if (binding != null) {
				ITypeBinding baseType= binding.isArray() ? binding.getElementType() : binding;
				if (!baseType.isPrimitive()) {
					importEdit.addImport(Bindings.getFullyQualifiedName(baseType));
				}
				return binding;
			}
		}
		return null;
	}


}
