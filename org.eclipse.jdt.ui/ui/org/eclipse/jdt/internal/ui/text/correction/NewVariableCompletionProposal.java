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
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jdt.internal.core.JavaModel;
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
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class NewVariableCompletionProposal extends CUCorrectionProposal {
	
	private class AddLocalVariableEdit extends SimpleTextEdit {
		
		
		private String fContent;
		private ASTNode fAstRoot;
		private int fTabSize;
		private IMethod fMethod;

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
					offset= statement.getStartPosition();
					int startLine= buffer.getLineOfOffset(offset);
					String indentString= TextUtil.createIndentString(buffer.getLineIndent(startLine, fTabSize));
					insertString= fContent +  buffer.getLineDelimiter() + indentString;
				}
			}
			setTextRange(new TextRange(offset, 0));
			setText(fContent);
			super.connect(editor);
		}	
	}
	
	

	private boolean  fLocalVariable;
	private String fVariableName;
	private IMember fParentMember;

	public NewVariableCompletionProposal(IMember parentMember, ProblemPosition problemPos, String label, boolean localVariable, String variableName, int relevance) throws CoreException {
		super(label, problemPos, relevance);
		
		fLocalVariable= localVariable;
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

		if (fLocalVariable) {
			// new local variable
			changeElement.addTextEdit("Add local", createLocalVariableEdit(content, settings.tabWidth, selectedNode)); //$NON-NLS-1$
		} else {
			// new field
			changeElement.addTextEdit("Add field", createFieldEdit(content, settings.tabWidth)); //$NON-NLS-1$
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
	
	
	private TextEdit createLocalVariableEdit(String content, int tabWidth, ASTNode curr) throws CoreException {
		return new AddLocalVariableEdit(curr, content, tabWidth);
	}				
		
	
	
	private String generateStub(ImportEdit importEdit, ASTNode selectedNode) {
		StringBuffer buf= new StringBuffer();
		String varType= evaluateVariableType(importEdit, selectedNode);
		
		buf.append("private "); //$NON-NLS-1$
		buf.append(varType);
		buf.append(' ');
		buf.append(fVariableName);
		buf.append(";"); //$NON-NLS-1$
		return buf.toString();
	}
		
	/*
	 * @see ICompletionProposal#getImage()
	 */
	public Image getImage() {
		return JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC);
	}
	
	private String evaluateVariableType(ImportEdit importEdit, ASTNode selectedNode) {
		if (selectedNode != null) {
			ITypeBinding binding= ASTResolving.getTypeBinding(selectedNode);
			if (binding != null) {
				ITypeBinding baseType= binding.isArray() ? binding.getElementType() : binding;
				if (!baseType.isPrimitive()) {
					importEdit.addImport(Bindings.getFullyQualifiedName(baseType));
				}
				return binding.getName();
			}
		}
		return "Object"; //$NON-NLS-1$
	}


}
