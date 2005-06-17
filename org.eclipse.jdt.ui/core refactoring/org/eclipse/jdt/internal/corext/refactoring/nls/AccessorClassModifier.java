/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.nls;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Region;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.IJavaStatusConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;

public class AccessorClassModifier {

	private CompilationUnit fRoot;
	private AST fAst;
	private ASTRewrite fASTRewrite;
	private ListRewrite fListRewrite;
	private ICompilationUnit fCU;

	private AccessorClassModifier(ICompilationUnit cu) throws CoreException {

		fCU= cu;
		
		fRoot= JavaPlugin.getDefault().getASTProvider().getAST(cu, ASTProvider.WAIT_YES, null);
		fAst= fRoot.getAST();
		fASTRewrite= ASTRewrite.create(fAst);
		
		AbstractTypeDeclaration parent= null;
		if (fRoot.types().size() > 0) {
			parent= (AbstractTypeDeclaration)fRoot.types().get(0);
			fListRewrite= fASTRewrite.getListRewrite(parent, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		} else {
			IStatus status= new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IJavaStatusConstants.INTERNAL_ERROR, NLSMessages.AccessorClassModifier_missingType, null); 
			throw new CoreException(status);
		}
	}
	
	private TextEdit getTextEdit() throws CoreException {
		IDocument document= null;
		
		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		IPath path= fCU.getPath();
		
		if (manager != null && path != null) {
			manager.connect(path, null);
			try {
				ITextFileBuffer buffer= manager.getTextFileBuffer(path);
				if (buffer != null)
					document= buffer.getDocument();
			} finally {
				manager.disconnect(path, null);
			}
		}
		
		if (document == null)
			document= new Document(fCU.getSource());
		 
		return fASTRewrite.rewriteAST(document, fCU.getJavaProject().getOptions(true));
	}

	public static Change create(ICompilationUnit cu, NLSSubstitution[] subs) throws CoreException {

		AccessorClassModifier sourceModification= new AccessorClassModifier(cu);

		String message= Messages.format(NLSMessages.NLSSourceModifier_change_description, cu.getElementName()); 

		TextChange change= new CompilationUnitChange(message, cu);
		MultiTextEdit multiTextEdit= new MultiTextEdit();
		change.setEdit(multiTextEdit);

		for (int i= 0; i < subs.length; i++) {
			NLSSubstitution substitution= subs[i];
			int newState= substitution.getState();
			if (substitution.hasStateChanged()) {
				if (newState == NLSSubstitution.EXTERNALIZED) {
					if (substitution.getInitialState() == NLSSubstitution.INTERNALIZED)
						sourceModification.addKey(substitution, change);
				} else if (newState == NLSSubstitution.INTERNALIZED) {
					if (substitution.getInitialState() == NLSSubstitution.EXTERNALIZED)
						sourceModification.removeKey(substitution, change);
				} else if (newState == NLSSubstitution.IGNORED) {
					if (substitution.getInitialState() == NLSSubstitution.EXTERNALIZED)
						sourceModification.removeKey(substitution, change);
				}
			} else {
				if (newState == NLSSubstitution.EXTERNALIZED) {
					if (substitution.isKeyRename()) {
						sourceModification.renameKey(substitution, change);
					}
					if (substitution.isAccessorRename()) {
						// FIXME: need to verify
						sourceModification.replaceAccessor(substitution, change);
					}
				}
			}
		}

		change.addEdit(sourceModification.getTextEdit());
		
		return change;
	}
	
	private void removeKey(NLSSubstitution sub, TextChange change) throws CoreException {
		ASTNode node= findField(fRoot, sub.getKey());
		if (node == null)
			return;
		
		String name= Messages.format(NLSMessages.AccessorClassModifier_remove_entry, sub.getKey()); 
		TextEditGroup editGroup= new TextEditGroup(name);
		fListRewrite.remove(node, editGroup);
		change.addTextEditGroup(editGroup);
	}
	
	private void renameKey(NLSSubstitution sub, TextChange change) throws CoreException {
		ASTNode node= findField(fRoot, sub.getInitialKey());
		if (node == null)
			return;
		
		FieldDeclaration fieldDeclaration= getNewFinalStringFieldDeclaration(sub.getKey());
		
		String name= Messages.format(NLSMessages.AccessorClassModifier_replace_entry, sub.getKey()); 
		TextEditGroup editGroup= new TextEditGroup(name);
		fListRewrite.replace(node, fieldDeclaration, editGroup);
		
		change.addTextEditGroup(editGroup);
	}
	
	private ASTNode findField(ASTNode astRoot, final String name) {
		
		class STOP_VISITING extends RuntimeException {
			private static final long serialVersionUID= 1L;
		}
		
		final ASTNode[] result= new ASTNode[1];
		
		try {
			astRoot.accept(new ASTVisitor() {
				
				public boolean visit(VariableDeclarationFragment node) {
					if (name.equals(node.getName().getFullyQualifiedName())) {
						result[0]= node.getParent();
						throw new STOP_VISITING();
					}
					return true;	
				}
			});
		} catch (STOP_VISITING ex) {
			// stop visiting AST
		}
		
		return result[0];
	}
	
	private void addKey(NLSSubstitution sub, TextChange change) throws CoreException {
		
		if (fListRewrite == null)
			return;
		
		FieldDeclaration fieldDeclaration= getNewFinalStringFieldDeclaration(sub.getKey());
		
		String name= Messages.format(NLSMessages.AccessorClassModifier_add_entry, sub.getKey()); 
		TextEditGroup editGroup= new TextEditGroup(name);
		fListRewrite.insertLast(fieldDeclaration, editGroup);
		change.addTextEditGroup(editGroup);
	}

	private FieldDeclaration getNewFinalStringFieldDeclaration(String name) {
		VariableDeclarationFragment variableDeclarationFragment= fAst.newVariableDeclarationFragment();
		variableDeclarationFragment.setName(fAst.newSimpleName(name));
		
		FieldDeclaration fieldDeclaration= fAst.newFieldDeclaration(variableDeclarationFragment);
		fieldDeclaration.setType(fAst.newSimpleType(fAst.newSimpleName("String"))); //$NON-NLS-1$
		fieldDeclaration.modifiers().add(fAst.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
		fieldDeclaration.modifiers().add(fAst.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD));
		
		return fieldDeclaration;
	}

	/**
	 * @param substitution
	 * @param change
	 */
	private void replaceAccessor(NLSSubstitution substitution, TextChange change) {
		AccessorClassReference accessorClassRef= substitution.getAccessorClassReference();
		if (accessorClassRef != null) {
			Region region= accessorClassRef.getRegion();
			int len= accessorClassRef.getName().length();
			String[] args= {accessorClassRef.getName(), substitution.getUpdatedAccessor()};
			TextChangeCompatibility.addTextEdit(change, Messages.format(NLSMessages.NLSSourceModifier_replace_accessor, args), 
					new ReplaceEdit(region.getOffset(), len, substitution.getUpdatedAccessor())); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
	}
}
