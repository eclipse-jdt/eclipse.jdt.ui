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
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;

import org.eclipse.jdt.ui.JavaUI;

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
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
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
					String indentString= CodeFormatterUtil.createIndentString(buffer.getLineIndent(startLine, fTabSize));
					String lineDelim= buffer.getLineDelimiter();
					insertString= StubUtility.codeFormat(fContent, 0, lineDelim) +  lineDelim + indentString;
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
		private ICompilationUnit fCompilationUnit;

		public AddParameterEdit(ICompilationUnit cu, ASTNode astRoot, String content) {
			fAstRoot= astRoot;
			fContent= content;
			fCompilationUnit= cu;
		}
		
		/* non Java-doc
		 * @see TextEdit#getCopy
		 */
		public TextEdit copy() {
			return new AddParameterEdit(fCompilationUnit, fAstRoot, fContent);
		}
		
		/* non Java-doc
		 * @see TextEdit#connect
		 */
		public void connect(TextBufferEditor editor) throws CoreException {
			if (fAstRoot == null) {
				return;
			}
			
			int offset= 0;
			String insertString= ""; //$NON-NLS-1$
			
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
					insertString= ", " + fContent; //$NON-NLS-1$
				} else {
					SimpleName name= declaration.getName();
					try {
						IScanner scanner= ASTResolving.createScanner(fCompilationUnit, name.getStartPosition());
						int nextNoken= scanner.getNextToken();
						while (nextNoken != ITerminalSymbols.TokenNameLPAREN) {
							nextNoken= scanner.getNextToken();
							if (nextNoken == ITerminalSymbols.TokenNameEOF) {
								throw new CoreException(new Status(Status.ERROR, JavaUI.ID_PLUGIN, Status.ERROR, "Unexpected EOF while scanning", null)); //$NON-NLS-1$
							}
						}
						offset= scanner.getCurrentTokenEndPosition() + 1;
					} catch (InvalidInputException e) {
						throw new CoreException(new Status(Status.ERROR, JavaUI.ID_PLUGIN, Status.ERROR, "Exception while scanning", e)); //$NON-NLS-1$
					}					
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
	private SimpleName fNode;
	private IMember fParentMember;

	public NewVariableCompletionProposal(String label, int variableKind, SimpleName node, IMember parentMember, int relevance) throws CoreException {
		super(label, parentMember.getCompilationUnit(), relevance, null);
	
		fVariableKind= variableKind;
		fNode= node;
		fParentMember= parentMember;
		if (variableKind == FIELD) {
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC));
		} else {
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL));
		}
	}

	/*
	 * @see JavaCorrectionProposal#addEdits(CompilationUnitChange)
	 */
	protected void addEdits(CompilationUnitChange changeElement) throws CoreException {
		ICompilationUnit cu= changeElement.getCompilationUnit();
		
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		ImportEdit importEdit= new ImportEdit(cu, settings);

		String content= generateStub(importEdit, fNode);
		if (!importEdit.isEmpty()) {
			changeElement.addTextEdit("imports", importEdit); //$NON-NLS-1$
		}		

		if (fVariableKind == LOCAL) {
			// new local variable
			changeElement.addTextEdit("local", new AddLocalVariableEdit(fNode, content, settings.tabWidth)); //$NON-NLS-1$
		} else if (fVariableKind == FIELD) {
			// new field
			changeElement.addTextEdit("field", createFieldEdit(content, settings.tabWidth)); //$NON-NLS-1$
		} else if (fVariableKind == PARAM) {
			// new parameter
			changeElement.addTextEdit("parameter", new AddParameterEdit(cu, fNode, content)); //$NON-NLS-1$
		}
	}
	
	private TextEdit createFieldEdit(String content, int tabWidth) throws CoreException {
		boolean parentIsType= (fParentMember.getElementType() ==  IJavaElement.TYPE);
		
		IType type= parentIsType ? (IType) fParentMember : fParentMember.getDeclaringType();

		int insertPos= MemberEdit.ADD_AT_BEGINNING;
		IJavaElement anchor= type;
		if (!parentIsType && fParentMember.getElementType() != IJavaElement.FIELD) {
			IField[] fields= type.getFields();
			if (fields.length > 0) {
				anchor= fields[fields.length - 1];
				insertPos= MemberEdit.INSERT_AFTER;
			}
		}

		MemberEdit memberEdit= new MemberEdit(anchor, insertPos, new String[] { content }, tabWidth);
		memberEdit.setUseFormatter(true);
		
		return memberEdit;
	}	


	private String generateStub(ImportEdit importEdit, SimpleName selectedNode) throws CoreException {
		StringBuffer buf= new StringBuffer();
		ITypeBinding varType= evaluateVariableType(importEdit, selectedNode);
				
		if (fVariableKind == FIELD) {
			buf.append("private "); //$NON-NLS-1$
			if (ASTResolving.isInStaticContext(selectedNode)) {
				buf.append("static "); //$NON-NLS-1$
			}			
		}
		
		buf.append(varType != null ? varType.getName() : "Object"); //$NON-NLS-1$
		buf.append(' ');
		buf.append(selectedNode.getIdentifier());
		if (fVariableKind == LOCAL) {
			if (varType == null || !varType.isPrimitive()) {
				buf.append("= null"); //$NON-NLS-1$
			} else if (varType.getName().equals("boolean")) { //$NON-NLS-1$
				buf.append("= false"); //$NON-NLS-1$
			} else {
				buf.append("= 0"); //$NON-NLS-1$
			}
		}
		if (fVariableKind != PARAM) {
			buf.append(";"); //$NON-NLS-1$
		}
			
		return buf.toString();
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
