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
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.ui.CodeGeneration;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.javadoc.JavaDocAccess;
import org.eclipse.jdt.internal.corext.javadoc.JavaDocTag;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 *
 */
public class JavadocTagsSubProcessor {

	
	public static void getMissingJavadocCommentProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		ASTNode node= problem.getCoveringNode(context.getASTRoot());
		if (node == null) {
			return;
		}
		BodyDeclaration declaration= ASTResolving.findParentBodyDeclaration(node);
		if (declaration == null) {
			return;
		}
		ICompilationUnit cu= context.getCompilationUnit();
		ITypeBinding binding= Bindings.getBindingOfParentType(declaration);
		if (binding == null) {
			return;
		}
		
		
		if (declaration instanceof MethodDeclaration) {
			MethodDeclaration methodDecl= (MethodDeclaration) declaration;
			IMethodBinding methodBinding= methodDecl.resolveBinding();
			if (methodBinding != null) {
				methodBinding= Bindings.findDeclarationInHierarchy(binding, methodBinding.getName(), methodBinding.getParameterTypes());
			}

			String string= CodeGeneration.getMethodComment(cu, binding.getName(), methodDecl, methodBinding, String.valueOf('\n'));
			if (string != null) {
				String label= CorrectionMessages.getString("JavadocTagsSubProcessor.addjavadoc.method.description"); //$NON-NLS-1$
				proposals.add(getNewJavadocTagProposal(cu, declaration.getStartPosition(), string, label));
			}
		} else if (declaration instanceof TypeDeclaration) {
			String typeQualifiedName= Bindings.getTypeQualifiedName(binding);
			
			String string= CodeGeneration.getTypeComment(cu, typeQualifiedName, String.valueOf('\n'));
			if (string != null) {
				String label= CorrectionMessages.getString("JavadocTagsSubProcessor.addjavadoc.type.description"); //$NON-NLS-1$
				proposals.add(getNewJavadocTagProposal(cu, declaration.getStartPosition(), string, label));
			}
		} else if (declaration instanceof FieldDeclaration) {
			 String comment= "/**\n *\n */\n"; //$NON-NLS-1$
			 String label= CorrectionMessages.getString("JavadocTagsSubProcessor.addjavadoc.field.description"); //$NON-NLS-1$
			 proposals.add(getNewJavadocTagProposal(cu, declaration.getStartPosition(), comment, label));
		}
	}

	private static CUCorrectionProposal getNewJavadocTagProposal(ICompilationUnit cu, int insertPosition, String comment, String label) throws CoreException {
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVADOCTAG);
		
		CUCorrectionProposal proposal= new CUCorrectionProposal(label, cu, 1, image);
		TextBuffer buffer= TextBuffer.acquire((IFile) cu.getResource());
		try {
			int tabWidth= CodeFormatterUtil.getTabWidth();
			int line= buffer.getLineOfOffset(insertPosition);
			String lineContent= buffer.getLineContent(line);
			String indentString= Strings.getIndentString(lineContent, tabWidth);
			String str= Strings.changeIndent(comment, 0, tabWidth, indentString, buffer.getLineDelimiter());
			TextEdit rootEdit= proposal.getRootTextEdit();
			InsertEdit edit= new InsertEdit(insertPosition, str);
			rootEdit.addChild(edit); //$NON-NLS-1$
			if (comment.charAt(comment.length() - 1) != '\n') {
				rootEdit.addChild(new InsertEdit(insertPosition, buffer.getLineDelimiter())); 
				rootEdit.addChild(new InsertEdit(insertPosition, indentString));
			}
			
			return proposal;
		} finally {
			TextBuffer.release(buffer);
		}
	}
	
	public static void getMissingJavadocTagProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		ASTNode node= problem.getCoveringNode(context.getASTRoot());
		if (node == null) {
			return;
		}
		
		BodyDeclaration declaration= ASTResolving.findParentBodyDeclaration(node);
		if (!(declaration instanceof MethodDeclaration)) {
			return;
		}
		
		Javadoc javadoc= declaration.getJavadoc();
		if (javadoc == null) {
			return;
		}
		
		MethodDeclaration methodDecl= (MethodDeclaration) declaration;
		
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVADOCTAG);
		JavadocRewriteProposal proposal= new JavadocRewriteProposal("", context.getCompilationUnit(), javadoc, 1, image); //$NON-NLS-1$
	
		JavaDocTag[] existingTags= proposal.getTags();
		
		switch (problem.getProblemId()) {
			case IProblem.AnnotationMissingParamTag: {
				String name= ASTNodes.asString(node);
				proposal.setDisplayName(CorrectionMessages.getString("JavadocTagsSubProcessor.addjavadoc.paramtag.description")); //$NON-NLS-1$
				int insertPosition= findParamInsertPosition(existingTags, methodDecl, node.getParent());
				proposal.insertNewTag(insertPosition, new JavaDocTag(JavaDocTag.PARAM, name));
				break;
			}
			case IProblem.AnnotationMissingReturnTag: {
				proposal.setDisplayName(CorrectionMessages.getString("JavadocTagsSubProcessor.addjavadoc.returntag.description")); //$NON-NLS-1$
				int insertPosition= findReturnInsertPosition(existingTags);
				proposal.insertNewTag(insertPosition, new JavaDocTag(JavaDocTag.RETURN, "")); //$NON-NLS-1$
				break;
			}
			case IProblem.AnnotationMissingThrowsTag: {
				String name= ASTNodes.asString(node);
				proposal.setDisplayName(CorrectionMessages.getString("JavadocTagsSubProcessor.addjavadoc.throwstag.description")); //$NON-NLS-1$
				int insertPosition= findThrowsInsertPosition(existingTags, methodDecl, node);
				proposal.insertNewTag(insertPosition, new JavaDocTag(JavaDocTag.THROWS, name));
				break;
			}
			default:
				return;
		}		
		proposals.add(proposal);
		
		String label= CorrectionMessages.getString("JavadocTagsSubProcessor.addjavadoc.allmissing.description"); //$NON-NLS-1$
		JavadocRewriteProposal addAllMissing= new JavadocRewriteProposal(label, context.getCompilationUnit(), javadoc, 1, image); //$NON-NLS-1$
		List list= methodDecl.parameters();
		for (int i= 0; i < list.size(); i++) {
			SingleVariableDeclaration decl= (SingleVariableDeclaration) list.get(i);
			String name= decl.getName().getIdentifier();
			if (findTag(existingTags, JavaDocTag.PARAM, name) == null) {
				int insertPosition= findParamInsertPosition(existingTags, methodDecl, decl);
				addAllMissing.insertNewTag(insertPosition, new JavaDocTag(JavaDocTag.PARAM, name)); //$NON-NLS-1$
			}
		}
		if (!methodDecl.isConstructor()) {
			Type type= methodDecl.getReturnType();
			if (!type.isPrimitiveType() || (((PrimitiveType) type).getPrimitiveTypeCode() != PrimitiveType.VOID)) {
				if (findTag(existingTags, JavaDocTag.RETURN, null) == null) {
					int insertPosition= findReturnInsertPosition(existingTags);
					addAllMissing.insertNewTag(insertPosition, new JavaDocTag(JavaDocTag.RETURN, "")); //$NON-NLS-1$
				}
			}
		}
		List throwsExceptions= methodDecl.thrownExceptions();
		for (int i= 0; i < throwsExceptions.size(); i++) {
			Name exception= (Name) throwsExceptions.get(i);
			ITypeBinding binding= exception.resolveTypeBinding();
			if (binding != null) {
				String name= binding.getName();
				if (findThrowsTag(existingTags, name) == null) {
					int insertPosition= findThrowsInsertPosition(existingTags, methodDecl, exception);
					addAllMissing.insertNewTag(insertPosition, new JavaDocTag(JavaDocTag.THROWS, name)); //$NON-NLS-1$
				}
			}
		}
		proposals.add(addAllMissing);
	}
	
	private static JavaDocTag findTag(JavaDocTag[] existingTags, String name, String arg) {
		for (int i= 0; i < existingTags.length; i++) {
			JavaDocTag curr= existingTags[i];
			if (curr.getName().equals(name)) {
				if (arg != null) {
					String argument= getArgument(curr.getContent());
					if (argument.equals(arg)) {
						return curr;
					}
				} else {
					return curr;
				}
			}
		}
		return null;
	}
	
	private static JavaDocTag findThrowsTag(JavaDocTag[] existingTags, String arg) {
		for (int i= 0; i < existingTags.length; i++) {
			JavaDocTag curr= existingTags[i];
			String currName= curr.getName();
			if (currName.equals(JavaDocTag.THROWS) || currName.equals(JavaDocTag.EXCEPTION)) {
				String argument= getArgument(curr.getContent());
				if (argument.equals(arg) || (Signature.getSimpleName(argument)).equals(arg)) {
					return curr;
				}
			}
		}
		return null;
	}
	
	
	private static int findThrowsInsertPosition(JavaDocTag[] tags, MethodDeclaration methodDecl, ASTNode node) {
		Set previousArgs= new HashSet();
		List list= methodDecl.thrownExceptions();
		for (int i= 0; i < list.size() && node != list.get(i); i++) {
			Name curr= (Name) list.get(i);
			previousArgs.add(ASTResolving.getSimpleName(curr));
		}
		int lastThrows= tags.length;
		for (int i= tags.length - 1; i >= 0; i--) {
			JavaDocTag curr= tags[i];
			if (JavaDocTag.THROWS.equals(curr.getName())) {
				String arg= getArgument(curr.getContent());
				if (previousArgs.contains(arg) || previousArgs.contains(Signature.getSimpleName(arg))) {
					return i + 1;
				}
				lastThrows= i;
			}
		}
		return lastThrows;
	}

	private static int findReturnInsertPosition(JavaDocTag[] tags) {
		int res= tags.length;
		for (int i= tags.length - 1; i >= 0; i--) {
			JavaDocTag curr= tags[i];
			if (JavaDocTag.THROWS.equals(curr.getName())) {
				res= i;
			} else if (JavaDocTag.PARAM.equals(curr.getName())) {
				return i + 1;
			}
		}
		return res;
	}

	private static int findParamInsertPosition(JavaDocTag[] tags, MethodDeclaration methodDecl, ASTNode node) {
		Set previousArgs= new HashSet();
		List list= methodDecl.parameters();
		for (int i= 0; i < list.size() && (list.get(i) != node); i++) {
			SingleVariableDeclaration curr= (SingleVariableDeclaration) list.get(i);
			previousArgs.add(curr.getName().getIdentifier());
		}
		for (int i= tags.length - 1; i >= 0; i--) {
			JavaDocTag curr= tags[i];
			if (JavaDocTag.PARAM.equals(curr.getName())) {
				String arg= getArgument(curr.getContent());
				if (previousArgs.contains(arg)) {
					return i + 1;
				}
			}
		}
		if (tags.length > 0 && tags[0].getName() == null) {
			return 1;
		}
		return 0;
	}
	
	private static String getArgument(String content) {
		int i= 0;
		while (i < content.length() && !Character.isWhitespace(content.charAt(i))) {
			i++;
		}
		return content.substring(0, i);
	}



	private static class JavadocRewriteProposal extends CUCorrectionProposal {

		private static class Event {
			JavaDocTag originalTag;
			JavaDocTag newTag;

			public Event(JavaDocTag originalTag, JavaDocTag newTag) {
				this.originalTag= originalTag;
				this.newTag= newTag;
			}
			
			public boolean isInserted() {
				return originalTag == null && newTag != null;
			}
			
			public boolean isRemoved() {
				return originalTag != null && newTag == null;
			}
			
		}
		
		private Javadoc fJavadoc;
		private JavaDocTag[] fOriginalTags;
		private List fChangedList;
		

		public JavadocRewriteProposal(String name, ICompilationUnit cu, Javadoc javadoc, int relevance, Image image) throws JavaModelException {
			super(name, cu, relevance, image);
			fJavadoc= javadoc;
			fOriginalTags= JavaDocAccess.getJavaDocTags(cu, javadoc.getStartPosition(), javadoc.getLength());
			fChangedList= new ArrayList();
			for (int i= 0; i < fOriginalTags.length; i++) {
				JavaDocTag curr= fOriginalTags[i];
				fChangedList.add(new Event(curr, curr));
			}
		
		}
		
		public JavaDocTag[] getTags() {
			return fOriginalTags;
		}

		public void insertNewTag(int index, JavaDocTag tag) {
			if (index < 0 || index > fOriginalTags.length) {
				throw new IllegalArgumentException();
			}
			int count= 0;
			
			// insert after all other previously insted tags
			int insertPos;
			for (insertPos= 0; insertPos < fChangedList.size(); insertPos++) {
				Event curr= (Event) fChangedList.get(insertPos);
				if (!curr.isInserted()) {
					if (count == index) {
						break;
					}
					count++;
				}
			}
			fChangedList.add(insertPos, new Event(null, tag));
		}	
		
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal#createCompilationUnitChange(String, ICompilationUnit, TextEdit)
		 */
		protected CompilationUnitChange createCompilationUnitChange(String name, ICompilationUnit cu, TextEdit rootEdit) throws CoreException {
			CompilationUnitChange change= super.createCompilationUnitChange(name, cu, rootEdit);
			TextBuffer buffer= null;
			try {
				buffer= TextBuffer.acquire(change.getFile());
				createEdits(buffer, cu, rootEdit);
			} finally {
				if (buffer != null) {
					TextBuffer.release(buffer);
				}
			}
			return change;
		}
		
		
/*		private void createEditsNew(TextBuffer buffer, ICompilationUnit cu, TextEdit root) throws JavaModelException {
			IBuffer cuBuffer= cu.getBuffer();
			String comment= cuBuffer.getText(fJavadoc.getStartPosition(), fJavadoc.getLength());
			
			try {
				String cat= "cat"; //$NON-NLS-1$
				
				Document doc= new Document(comment);
				doc.addPositionCategory(cat);
				
				int offset= fJavadoc.getStartPosition();
				
				int currPos= 3;
				for (int i= fChangedList.size() - 1; i > 0; i--) {
					Event curr= (Event) fChangedList.get(i);
					JavaDocTag origTag= curr.originalTag;
					JavaDocTag newTag= curr.newTag;
					if (origTag == newTag) {
						int start= origTag.getOffset();
						int end= origTag.getContentOffset() + origTag.getContentLength();
						doc.addPosition(cat, new Position(start  - offset, end - start));
						currPos= origTag.getOffset() + origTag.getLength() - offset;
					} else if (newTag == null) {
						// remove
						doc.replace(origTag.getOffset() - offset, origTag.getLength(), ""); //$NON-NLS-1$
						currPos= origTag.getOffset() + origTag.getLength() - offset;
					} else if (origTag == null) {
						// insert
						String string= getNewTagString(curr.newTag);
						doc.replace(currPos, 0, string);
					} else {
						// replace
						String string= getNewTagString(curr.newTag);
						doc.replace(origTag.getOffset() - offset, origTag.getLength(), string); //$NON-NLS-1$
					}
				}
				
				Position[] positions= doc.getPositions(cat);
				String str= doc.get();
				System.out.println(str);
				System.out.println("++++++++"); //$NON-NLS-1$
				for (int i= 0; i < positions.length; i++) {
					Position pos= positions[i];
					String string= doc.get(pos.getOffset(), pos.getLength());
					System.out.println(string);
				}
				
			} catch (BadLocationException e) {
				
			} catch (BadPositionCategoryException e) {
			}
		}*/
		
		
		private void createEdits(TextBuffer buffer, ICompilationUnit cu, TextEdit root) {
			
			//createEditsNew(buffer, cu, root);
		
			int currPos= fJavadoc.getStartPosition() + 3;
			boolean needsLead= true;
			boolean isLast= false;
			for (int i= 0; i < fChangedList.size(); i++) {
				Event curr= (Event) fChangedList.get(i);
				if (curr.isInserted()) {
					StringBuffer buf= new StringBuffer();
					if (needsLead) {
						buf.append('\n');
					}
					buf.append(getNewTagString(curr.newTag));
					if (!needsLead && !isLast) {
						buf.append('\n');
					}
					
					String indentString= getIndent(buffer) + " * "; //$NON-NLS-1$
					String str= Strings.changeIndent(buf.toString(), 0, CodeFormatterUtil.getTabWidth(), indentString, buffer.getLineDelimiter());
					if (isLast) {
						str= "* " + str + buffer.getLineDelimiter() + getIndent(buffer) + ' ';//$NON-NLS-1$
					}
					root.addChild(new InsertEdit(currPos, str));
				} else {
					JavaDocTag original= curr.originalTag;
					currPos= original.getOffset() + original.getLength();
					needsLead= false;
					isLast= (fOriginalTags.length > 0 && fOriginalTags[fOriginalTags.length - 1] == original);
				}
			}
		}
		
		private String getNewTagString(JavaDocTag newTag) {
			StringBuffer buf= new StringBuffer();
			if (newTag.getName() != null) {
				buf.append('@');
				buf.append(newTag.getName());
				if (newTag.getContent().length() > 0) {
					buf.append(' ');
				}
			}
			buf.append(newTag.getContent());
			return buf.toString();
		}
		
		
		private String getIndent(TextBuffer buffer) {
			String line= buffer.getLineContentOfOffset(fJavadoc.getStartPosition());
			String indent= Strings.getIndentString(line, CodeFormatterUtil.getTabWidth());
			return indent;
		}
		
		
		
	}
	
	
	
	
		

	
}
