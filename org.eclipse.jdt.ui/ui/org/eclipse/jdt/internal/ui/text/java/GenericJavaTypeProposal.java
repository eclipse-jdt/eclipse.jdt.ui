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
package org.eclipse.jdt.internal.ui.text.java;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

import org.eclipse.jdt.core.CompletionContext;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jdt.internal.corext.template.java.SignatureUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorHighlightingSynchronizer;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * An experimental proposal.
 */
public final class GenericJavaTypeProposal extends JavaTypeCompletionProposal {
	
	static final class TypeArgumentProposal {
		private boolean fIsAmbiguous;
		private String[] fProposals;
		TypeArgumentProposal(String[] proposals, boolean ambiguous) {
			fIsAmbiguous= ambiguous;
			fProposals= proposals;
		}
		
		TypeArgumentProposal(String proposal, boolean ambiguous) {
			this(new String[] {proposal}, ambiguous);
		}
		
		boolean isAmbiguous() {
			return fIsAmbiguous;
		}
		
		String[] getProposals() {
			return fProposals;
		}
		
		public String toString() {
			return fProposals[0];
		}
	}

	private IRegion fSelectedRegion; // initialized by apply()
	private final CompletionContext fContext;
	private final CompletionProposal fProposal;

	public GenericJavaTypeProposal(CompletionProposal typeProposal, CompletionContext context, int offset, int length, ICompilationUnit cu, Image image, String displayString) {
		super(String.valueOf(typeProposal.getCompletion()), cu, offset, length, image, displayString, computeRelevance(typeProposal), String.valueOf(Signature.getSignatureSimpleName(typeProposal.getSignature())), String.valueOf(Signature.getSignatureQualifier(typeProposal.getSignature())));
		fProposal= typeProposal;
		fContext= context;
	}

	private static int computeRelevance(CompletionProposal typeProposal) {
		// TODO replace by CompletionProposalCollector.computeRelevance
		return typeProposal.getRelevance() * 16 + 3;
	}

	/*
	 * @see ICompletionProposalExtension#apply(IDocument, char)
	 */
	public void apply(IDocument document, char trigger, int offset) {
		
		TypeArgumentProposal[] typeArgumentProposals;
		try {
			typeArgumentProposals= computeTypeArgumentTypes();
		} catch (JavaModelException e) {
			super.apply(document, trigger, offset);
			return;
		}
		
		if (typeArgumentProposals.length == 0 || !shouldAppendArguments(document, offset)) {
			// not a parameterized type || already followed by generic signature
			super.apply(document, trigger, offset);
			return;
		}
		
		int[] offsets= new int[typeArgumentProposals.length];
		int[] lengths= new int[typeArgumentProposals.length];
		StringBuffer buffer= createParameterList(typeArgumentProposals, offsets, lengths);

		// set the generic type as replacement string
		super.setReplacementString(buffer.toString());
		// add import & remove package, update replacement offset
		super.apply(document, trigger, offset);

		if (fTextViewer != null) {
			String replacementString= getReplacementString();
			int delta= buffer.length() - replacementString.length(); // due to using an import instead of package
			for (int i= 0; i < offsets.length; i++) {
				offsets[i]-= delta;
			}
			installLinkedMode(document, offsets, lengths, typeArgumentProposals);
		}
	}
	
	private TypeArgumentProposal[] computeTypeArgumentTypes() throws JavaModelException {
		IType type= getProposedType();
		if (type == null)
			return new TypeArgumentProposal[0];
		
		ITypeParameter[] parameters= type.getTypeParameters();
		if (parameters.length == 0)
			return new TypeArgumentProposal[0];
		
		TypeArgumentProposal[] arguments= new TypeArgumentProposal[parameters.length];
		ITypeBinding declaredTypeBinding= getDeclaredType();
		if (declaredTypeBinding != null && declaredTypeBinding.isParameterizedType()) {
			// in this case, the type arguments we propose need to be compatible
			// with the corresponding type parameters to declared type 
			
			IType declaredType= (IType) declaredTypeBinding.getJavaElement();
			
			IType[] inheritanceChain= computeInheritance(type, declaredType);
			if (inheritanceChain == null)
				return new TypeArgumentProposal[0];
			
			int[] indices= new int[parameters.length];
			for (int i= 0; i < parameters.length; i++) {
				indices[i]= mapTypeParameterIndex(inheritanceChain, inheritanceChain.length - 1, i);
			}
			
			// for type arguments that are mapped through to the expected type's 
			// parameters, take the arguments of the declared type
			// for type arguments that are not mapped through, take the lower bound of the type parameter
			ITypeBinding[] typeArguments= declaredTypeBinding.getTypeArguments();
			for (int i= 0; i < parameters.length; i++) {
				if (indices[i] != -1) {
					// type argument is mapped through
					ITypeBinding binding= typeArguments[indices[i]];
					arguments[i]= computeTypeProposal(binding);
				}
			}
		}
		for (int i= 0; i < arguments.length; i++) {
			if (arguments[i] == null) {
				// not a mapped argument
				String[] bounds= parameters[i].getBounds();
				if (bounds.length > 0)
					arguments[i]= new TypeArgumentProposal(Signature.getSimpleName(bounds[0]), true); // take first bound if any
				else
					arguments[i]= new TypeArgumentProposal(parameters[i].getElementName(), true);
			}
		}
		
		return arguments;
	}
	
	private TypeArgumentProposal computeTypeProposal(ITypeBinding binding) {
		if (binding.isUpperbound()) {
			// upper bound - the upper bound is the bound itself
			return new TypeArgumentProposal(binding.getBound().getName(), true);
		} else if (binding.isWildcardType()) {
			// lower bound - the upper bound is always object
			return new TypeArgumentProposal("Object", true); //$NON-NLS-1$
		}
		
		// not a wildcard
		return new TypeArgumentProposal(binding.getName(), false);
	}

	private IType[] computeInheritance(IType subType, IType superType) throws JavaModelException {
		ITypeHierarchy hierarchy= subType.newSupertypeHierarchy(getProgressMonitor());
		if (!hierarchy.contains(superType))
			return null; // no path
		
		List inheritancePath= new LinkedList();
		inheritancePath.add(superType);
		while (!superType.equals(subType)) {
			// any sub type must be on a hierarchy chain from superType to subType
			superType= hierarchy.getSubtypes(superType)[0];
			inheritancePath.add(superType);
		}
		
		return (IType[]) inheritancePath.toArray(new IType[inheritancePath.size()]);
	}

	private NullProgressMonitor getProgressMonitor() {
		return new NullProgressMonitor();
	}

	private int mapTypeParameterIndex(IType[] inheritanceChain, int typeIndex, int paramIndex) throws JavaModelException {
		if (typeIndex == 0) {
			// break condition: we've reached the top of the hierarchy
			return paramIndex;
		}
		
		IType subType= inheritanceChain[typeIndex];
		IType superType= inheritanceChain[typeIndex - 1];
		
		String superSignature= findMatchingSuperTypeSignature(subType, superType);
		ITypeParameter param= subType.getTypeParameters()[paramIndex];
		int index= findMatchingTypeArgumentIndex(superSignature, param.getElementName());
		if (index == -1) {
			// not mapped through
			return -1;
		}
		
		return mapTypeParameterIndex(inheritanceChain, typeIndex - 1, index); 
	}
	

	/**
	 * Finds and returns the type argument with index <code>index</code>
	 * in the given type super type signature. If <code>signature</code>
	 * is a generic signature, the type parameter at <code>index</code> is
	 * extracted. If the type parameter is an upper bound (<code>? super SomeType</code>),
	 * the type signature of <code>java.lang.Object</code> is returned.
	 * <p>
	 * Also, if <code>signature</code> has no type parameters (i.e. is a
	 * reference to the raw type), the type signature of
	 * <code>java.lang.Object</code> is returned.
	 * </p>
	 * 
	 * @param signature the super type signature from a type's
	 *        <code>extends</code> or <code>implements</code> clause
	 * @param paramName the name of the type argument to find
	 */
	private int findMatchingTypeArgumentIndex(String signature, String paramName) {
		String[] typeArguments= Signature.getTypeArguments(signature);
		for (int i= 0; i < typeArguments.length; i++) {
			if (Signature.getSignatureSimpleName(typeArguments[i]).equals(paramName))
				return i;
		}
		return -1;
	}
	
	/**
	 * Finds and returns the super type signature in the
	 * <code>extends</code> or <code>implements</code> clause of
	 * <code>subType</code> that corresponds to <code>superType</code>.
	 * 
	 * @param subType a direct and true sub type of <code>superType</code>
	 * @param superType a direct super type (super class or interface) of
	 *        <code>subType</code>
	 * @return the super type signature of <code>subType</code> referring
	 *         to <code>superType</code>
	 * @throws JavaModelException if extracting the super type signatures
	 *         fails, or if <code>subType</code> contains no super type
	 *         signature to <code>superType</code>
	 */
	private String findMatchingSuperTypeSignature(IType subType, IType superType) throws JavaModelException {
		String[] signatures= getSuperTypeSignatures(subType, superType);
		for (int i= 0; i < signatures.length; i++) {
			String signature= signatures[i];
			String qualified= SignatureUtil.qualifySignature(signature, subType);
			String subFQN= SignatureUtil.stripSignatureToFQN(qualified);
			
			String superFQN= superType.getFullyQualifiedName();
			if (subFQN.equals(superFQN)) {
				return signature;
			}
			
			// TODO handle local types
		}
		
		throw new JavaModelException(new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, "Illegal hierarchy", null))); //$NON-NLS-1$
	}
	
	/**
	 * Returns the super interface signatures of <code>subType</code> if 
	 * <code>superType</code> is an interface, otherwise returns the super
	 * type signature.
	 * 
	 * @param subType the sub type signature
	 * @param superType the super type signature
	 * @return the super type signatures of <code>subType</code>
	 * @throws JavaModelException if any java model operation fails
	 */
	private String[] getSuperTypeSignatures(IType subType, IType superType) throws JavaModelException {
		if (superType.isInterface())
			return subType.getSuperInterfaceTypeSignatures();
		else
			return new String[] {subType.getSuperclassTypeSignature()};
	}

	private ITypeBinding getDeclaredType() {
		char[][] chKeys= fContext.getExpectedTypesKeys();
		if (chKeys == null || chKeys.length == 0)
			return null;
		
		String[] keys= new String[chKeys.length];
		for (int i= 0; i < keys.length; i++) {
			keys[i]= String.valueOf(chKeys[0]);
		}
		
		final ASTParser parser= ASTParser.newParser(AST.JLS3);
		parser.setProject(fCompilationUnit.getJavaProject());
		parser.setResolveBindings(true);
		
		final Map bindings= new HashMap();
		ASTRequestor requestor= new ASTRequestor() {
			public void acceptBinding(String bindingKey, IBinding binding) {
				bindings.put(bindingKey, binding);
			}
		};
		parser.createASTs(new ICompilationUnit[0], keys, requestor, null);
		
		if (bindings.size() > 0)
			return (ITypeBinding) bindings.get(keys[0]);

		return null;
	}

	private IType getProposedType() throws JavaModelException {
		if (fCompilationUnit != null) {
			String fullType= SignatureUtil.stripSignatureToFQN(String.valueOf(fProposal.getSignature()));
			return fCompilationUnit.getJavaProject().findType(fullType);
		}
		return null;
	}

	private boolean shouldAppendArguments(IDocument document, int offset) {
		try {
			IRegion region= document.getLineInformationOfOffset(offset);
			String line= document.get(region.getOffset(), region.getLength());
			
			int index= offset - region.getOffset();
			while (index != line.length() && Character.isUnicodeIdentifierPart(line.charAt(index)))
				++index;
			
			if (index == line.length())
				return true;
				
			char ch= line.charAt(index);
			return ch != '<';
		
		} catch (BadLocationException e) {
			return true;
		}
	}
	
	private StringBuffer createParameterList(TypeArgumentProposal[] typeArguments, int[] offsets, int[] lengths) {
		StringBuffer buffer= new StringBuffer();
		buffer.append(getReplacementString());
		buffer.append('<');
		for (int i= 0; i != typeArguments.length; i++) {
			if (i != 0)
				buffer.append(", "); //$NON-NLS-1$ // TODO respect formatter prefs
				
			offsets[i]= buffer.length();
			buffer.append(typeArguments[i]);
			lengths[i]= buffer.length() - offsets[i];
		}
		buffer.append('>');
		return buffer;
	}

	private void installLinkedMode(IDocument document, int[] offsets, int[] lengths, TypeArgumentProposal[] typeArgumentProposals) {
		int replacementOffset= getReplacementOffset();
		String replacementString= getReplacementString();

		boolean hasAmbiguousProposals= false;
		for (int i= 0; i < typeArgumentProposals.length; i++) {
			if (typeArgumentProposals[i].isAmbiguous()) {
				hasAmbiguousProposals= true;
				break;
			}
		}
		if (!hasAmbiguousProposals) {
			fSelectedRegion= new Region(replacementOffset + replacementString.length(), 0);
			return;
		}
		
		try {
			LinkedModeModel model= new LinkedModeModel();
			for (int i= 0; i != offsets.length; i++) {
				if (typeArgumentProposals[i].isAmbiguous()) {
					LinkedPositionGroup group= new LinkedPositionGroup();
					group.addPosition(new LinkedPosition(document, replacementOffset + offsets[i], lengths[i], LinkedPositionGroup.NO_STOP));
					model.addGroup(group);
				}
			}
			
			model.forceInstall();
			JavaEditor editor= getJavaEditor();
			if (editor != null) {
				model.addLinkingListener(new EditorHighlightingSynchronizer(editor));
			}
			
			LinkedModeUI ui= new EditorLinkedModeUI(model, fTextViewer);
			ui.setExitPosition(fTextViewer, replacementOffset + replacementString.length(), 0, Integer.MAX_VALUE);
			ui.setDoContextInfo(true);
			ui.enter();

			fSelectedRegion= ui.getSelectedRegion();

		} catch (BadLocationException e) {
			JavaPlugin.log(e);	
			openErrorDialog(e);
		}
	}
	
	/**
	 * Returns the currently active java editor, or <code>null</code> if it 
	 * cannot be determined.
	 * 
	 * @return  the currently active java editor, or <code>null</code>
	 */
	private JavaEditor getJavaEditor() {
		IEditorPart part= JavaPlugin.getActivePage().getActiveEditor();
		if (part instanceof JavaEditor)
			return (JavaEditor) part;
		else
			return null;
	}

	/*
	 * @see ICompletionProposal#getSelection(IDocument)
	 */
	public Point getSelection(IDocument document) {
		if (fSelectedRegion == null)
			return super.getSelection(document);

		return new Point(fSelectedRegion.getOffset(), fSelectedRegion.getLength());
	}

	private void openErrorDialog(BadLocationException e) {
		Shell shell= fTextViewer.getTextWidget().getShell();
		MessageDialog.openError(shell, JavaTextMessages.getString("ExperimentalProposal.error.msg"), e.getMessage()); //$NON-NLS-1$
	}	

}
