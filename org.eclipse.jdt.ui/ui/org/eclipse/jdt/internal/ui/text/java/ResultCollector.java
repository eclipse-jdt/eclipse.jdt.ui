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

  
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.ITextViewer;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.internal.corext.template.java.SignatureUtil;
import org.eclipse.jdt.internal.corext.util.TypeFilter;

import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;

/**
 * Java implementation of <code>CompletionRequestor</code>.
 * 
 * @since 3.1
 */
public class ResultCollector extends CompletionRequestor {

	/** Tells whether this class is in debug mode. */
	private static final boolean DEBUG= "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.jdt.ui/debug/ResultCollector"));  //$NON-NLS-1$//$NON-NLS-2$

	protected final static char[] METHOD_WITH_ARGUMENTS_TRIGGERS= new char[] { '(', '-', ' ' };
	protected final static char[] METHOD_TRIGGERS= new char[] { ';', ',', '.', '\t', '[', ' ' };
	protected final static char[] TYPE_TRIGGERS= new char[] { '.', '\t', '[', '(', ' ' };
	protected final static char[] VAR_TRIGGER= new char[] { '\t', ' ', '=', ';', '.' };

	private final List 
		fFields= new ArrayList(),
		fKeywords= new ArrayList(10), 
		fLabels= new ArrayList(10),
		fMethods= new ArrayList(),
		fModifiers= new ArrayList(10),
		fPackages= new ArrayList(),
		fTypes= new ArrayList(),
		fVariables= new ArrayList();

	
	private final List[] fResults = new List[] {
		fPackages, fLabels, fModifiers, fKeywords, fTypes, fMethods, fFields, fVariables
	};
	
	private final Set fSuggestedMethodNames= new HashSet();
	private final ImageDescriptorRegistry fRegistry= JavaPlugin.getImageDescriptorRegistry();
	private final ProposalLabelProvider fLabelProvider= new ProposalLabelProvider();
	
	private IJavaProject fJavaProject;
	private ICompilationUnit fCompilationUnit; // set when imports can be added
	private int fCodeAssistOffset;
	private int fContextOffset;
	private ITextViewer fTextViewer;
	private int fUserReplacementLength;
	private IProblem fLastProblem;	
	
	/*
	 * Is eating code assist enabled or disabled? PR #3666
	 * When eating is enabled, JavaCompletionProposal must be revisited: PR #5533
	 */
	private boolean fPreventEating= true;
//	private CompletionContext fContext;
	private long fStartTime;
	private long fUITime;	
	
	private void acceptType(CompletionProposal proposal) {
		IJavaCompletionProposal uiProposal= createTypeProposal(proposal);
		if (uiProposal != null)
			fTypes.add(uiProposal);
	}
	
	protected IJavaCompletionProposal createTypeProposal(CompletionProposal typeProposal) {
		char[] signature= typeProposal.getSignature();
		String packageName= String.valueOf(Signature.getSignatureQualifier(signature)); 
		String typeName= String.valueOf(Signature.getSignatureSimpleName(signature));
		
		String completion= String.valueOf(typeProposal.getCompletion());
		int start= typeProposal.getReplaceStart();
		int end= typeProposal.getReplaceEnd();
		ImageDescriptor descriptor= fLabelProvider.createImageDescriptor(typeProposal);
		String label= fLabelProvider.createLabel(typeProposal);
		int relevance= typeProposal.getRelevance();

		JavaCompletionProposal proposal= new JavaTypeCompletionProposal(completion, fCompilationUnit, start, getLength(start, end), getImage(descriptor), label, relevance, typeName, packageName);

		ProposalInfo info= new TypeProposalInfo(fJavaProject, typeProposal);
		proposal.setProposalInfo(info);
		
		proposal.setTriggerCharacters(TYPE_TRIGGERS);
		
		return proposal;
	}
	
	private void acceptField(CompletionProposal proposal) {
		IJavaCompletionProposal uiProposal= createFieldProposal(proposal);
		if (uiProposal != null)
			fFields.add(uiProposal);
	}
	protected IJavaCompletionProposal createFieldProposal(CompletionProposal proposal) {
		String completion= String.valueOf(proposal.getCompletion());
		int start= proposal.getReplaceStart();
		int length= getLength(start, proposal.getReplaceEnd());
		String label= fLabelProvider.createLabelWithTypeAndDeclaration(proposal);
		Image image= getImage(fLabelProvider.createFieldImageDescriptor(proposal));
		int relevance= proposal.getRelevance();

		JavaCompletionProposal javaProposal= new JavaCompletionProposal(completion, start, length, image, label, relevance, fTextViewer);
		javaProposal.setProposalInfo(new FieldProposalInfo(fJavaProject, proposal));
		javaProposal.setTriggerCharacters(VAR_TRIGGER);
		
		return javaProposal;
	}
	
	private void acceptAnonymousType(CompletionProposal proposal) {
		IJavaCompletionProposal uiProposal= createAnonymousTypeProposal(proposal);
		if (uiProposal != null)
			fTypes.add(uiProposal);
	}
	protected IJavaCompletionProposal createAnonymousTypeProposal(CompletionProposal proposal) {
		if (fCompilationUnit == null)
			return null;
		
		String declaringType= SignatureUtil.stripSignatureToFQN(String.valueOf(proposal.getDeclarationSignature()));
		String completion= String.valueOf(proposal.getCompletion());
		int start= proposal.getReplaceStart();
		int length= getLength(start, proposal.getReplaceEnd());
		int relevance= proposal.getRelevance();
		
		String label= fLabelProvider.createAnonymousTypeLabel(proposal);
		
		JavaCompletionProposal javaProposal= new AnonymousTypeCompletionProposal(fJavaProject, fCompilationUnit, start, length, completion, label, declaringType, relevance);
		javaProposal.setProposalInfo(new AnonymousTypeProposalInfo(fJavaProject, proposal));
		return javaProposal;
	}	
	
	private void acceptKeyword(CompletionProposal proposal) {
		IJavaCompletionProposal uiProposal= createKeywordProposal(proposal);
		if (uiProposal != null)
			fKeywords.add(uiProposal);
	}
	protected IJavaCompletionProposal createKeywordProposal(CompletionProposal proposal) {
		String completion= String.valueOf(proposal.getCompletion());
		int start= proposal.getReplaceStart();
		int length= getLength(start, proposal.getReplaceEnd());
		String label= fLabelProvider.createSimpleLabel(proposal);
		int relevance= proposal.getRelevance();
		return new JavaCompletionProposal(completion, start, length, null, label, relevance, fTextViewer);
	}
	
	private void acceptLabel(CompletionProposal proposal) {
		IJavaCompletionProposal uiProposal= createLabelProposal(proposal);
		if (uiProposal != null)
			fLabels.add(uiProposal);
	}
	protected IJavaCompletionProposal createLabelProposal(CompletionProposal proposal) {
		String completion= String.valueOf(proposal.getCompletion());
		int start= proposal.getReplaceStart();
		int length= getLength(start, proposal.getReplaceEnd());
		String label= fLabelProvider.createSimpleLabel(proposal);
		int relevance= proposal.getRelevance();
		
		return new JavaCompletionProposal(completion, start, length, null, label, relevance, fTextViewer);
	}
	
	private void acceptLocalVariable(CompletionProposal proposal) {
		IJavaCompletionProposal uiProposal= createLocalVariableProposal(proposal);
		if (uiProposal != null)
			fVariables.add(uiProposal);
	}
	protected IJavaCompletionProposal createLocalVariableProposal(CompletionProposal proposal) {
		String completion= String.valueOf(proposal.getCompletion());
		int start= proposal.getReplaceStart();
		int length= getLength(start, proposal.getReplaceEnd());
		Image image= getImage(fLabelProvider.createLocalImageDescriptor(proposal));
		String label= fLabelProvider.createSimpleLabelWithType(proposal);
		int relevance= proposal.getRelevance();
		
		final JavaCompletionProposal javaProposal= new JavaCompletionProposal(completion, start, length, image, label, relevance, fTextViewer);
		javaProposal.setTriggerCharacters(VAR_TRIGGER);
		return javaProposal;
	}
	
	/**
	 * Creates a completion proposal (UI) given a completion proposal of type
	 * <code>CompletionProposal.METHOD_REF</code> (core).
	 * 
	 * @param proposal the method proposal to accept
	 */
	private void acceptMethodReference(CompletionProposal proposal) {
		IJavaCompletionProposal uiProposal= createMethodReferenceProposal(proposal);
		if (uiProposal != null)
			fMethods.add(uiProposal);
	}
	protected IJavaCompletionProposal createMethodReferenceProposal(CompletionProposal methodProposal) {
		Image image= getImage(fLabelProvider.createImageDescriptor(methodProposal));
		String displayName= fLabelProvider.createLabel(methodProposal);
		String completion= String.valueOf(methodProposal.getCompletion());
		int start= methodProposal.getReplaceStart();
		int end= methodProposal.getReplaceEnd();
		int relevance= methodProposal.getRelevance();
		
		JavaCompletionProposal proposal= new JavaCompletionProposal(completion, start, getLength(start, end), image, displayName, relevance, fTextViewer);
		
		proposal.setProposalInfo(new MethodProposalInfo(fJavaProject, methodProposal));

		char[] completionName= methodProposal.getCompletion();
		boolean hasParameters= Signature.getParameterCount(methodProposal.getSignature()) > 0;
		if (hasParameters) {
			ProposalContextInformation contextInformation= createContextInformation(methodProposal);
			proposal.setContextInformation(contextInformation);
		
			proposal.setTriggerCharacters(METHOD_WITH_ARGUMENTS_TRIGGERS);
			
			if (completionName.length > 0) {
				// set the cursor before the closing bracket
				proposal.setCursorPosition(completionName.length - 1);
			}
		} else {
			proposal.setTriggerCharacters(METHOD_TRIGGERS);
		}
		
		return proposal;
	}
	
	protected ProposalContextInformation createContextInformation(CompletionProposal proposal) {
		ProposalContextInformation contextInformation= new ProposalContextInformation(proposal);
		char[] completionName= proposal.getCompletion();
		contextInformation.setContextInformationPosition(completionName.length == 0 ? fContextOffset : -1);
		return contextInformation;
	}
	
	private void acceptPackage(CompletionProposal proposal) {
		IJavaCompletionProposal uiProposal= createPackageProposal(proposal);
		if (uiProposal != null)
			fPackages.add(uiProposal);
	}
	protected IJavaCompletionProposal createPackageProposal(CompletionProposal proposal) {
		String completion= String.valueOf(proposal.getCompletion());
		int start= proposal.getReplaceStart();
		int length= getLength(start, proposal.getReplaceEnd());
		String label= fLabelProvider.createSimpleLabel(proposal);
		Image image= getImage(fLabelProvider.createPackageImageDescriptor(proposal));
		int relevance= proposal.getRelevance();

		return new JavaCompletionProposal(completion, start, length, image, label, relevance, fTextViewer);
	}
	
	/*
	 * @see ICodeCompletionRequestor#acceptMethodDeclaration
	 */
	private void acceptMethodDeclaration(CompletionProposal proposal) {
		IJavaCompletionProposal uiProposal= createMethodDeclarationProposal(proposal);
		if (uiProposal != null)
			fMethods.add(uiProposal);
	}
	protected IJavaCompletionProposal createMethodDeclarationProposal(CompletionProposal proposal) {
		if (fCompilationUnit == null)
			return null;
		
		String name= String.valueOf(proposal.getName());
		String[] paramTypes= Signature.getParameterTypes(String.valueOf(proposal.getSignature()));
		for (int index= 0; index < paramTypes.length; index++)
			paramTypes[index]= Signature.toString(paramTypes[index]);
		String completion= String.valueOf(proposal.getCompletion());
		int start= proposal.getReplaceStart();
		int length= getLength(start, proposal.getReplaceEnd());
		
		String label= fLabelProvider.createOverrideMethodProposalLabel(proposal);

		JavaCompletionProposal javaProposal= new OverrideCompletionProposal(fJavaProject, fCompilationUnit, name, paramTypes, start, length, label, completion);
		javaProposal.setImage(getImage(fLabelProvider.createMethodImageDescriptor(proposal)));
		javaProposal.setProposalInfo(new MethodProposalInfo(fJavaProject, proposal));
		javaProposal.setRelevance(proposal.getRelevance());

		fSuggestedMethodNames.add(new String(name));
		return javaProposal;
	}
	
	/*
	 * @see IExtendedCompletionRequestor#acceptPotentialMethodDeclaration
	 */
	private void acceptPotentialMethodDeclaration(CompletionProposal proposal) {
		if (fCompilationUnit == null)
			return;
		
		String prefix= String.valueOf(proposal.getName());
		int completionStart= proposal.getReplaceStart();
		int completionEnd= proposal.getReplaceEnd();
		int relevance= proposal.getRelevance();
	
		try {
			IJavaElement element= fCompilationUnit.getElementAt(fCodeAssistOffset);
			if (element != null) {
				IType type= (IType) element.getAncestor(IJavaElement.TYPE);
				if (type != null) {
					GetterSetterCompletionProposal.evaluateProposals(type, prefix, completionStart, completionEnd - completionStart, relevance + 100, fSuggestedMethodNames, fMethods);
					MethodCompletionProposal.evaluateProposals(type, prefix, completionStart, completionEnd - completionStart, relevance + 99, fSuggestedMethodNames, fMethods);
				}
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
	}
	
	public String getErrorMessage() {
		if (fLastProblem != null)
			return fLastProblem.getMessage();
		return ""; //$NON-NLS-1$
	}

	public JavaCompletionProposal[] getResults() {
		// return unsorted
		int totLen= 0;
		for (int i= 0; i < fResults.length; i++) {
			totLen += fResults[i].size();
		}
		JavaCompletionProposal[] result= new JavaCompletionProposal[totLen];
		int k= 0;
		for (int i= 0; i < fResults.length; i++) {
			List curr= fResults[i];
			int currLen= curr.size();
			for (int j= 0; j < currLen; j++) {
				JavaCompletionProposal proposal= (JavaCompletionProposal) curr.get(j);
				// for equal relevance, take categories
				proposal.setRelevance(proposal.getRelevance() * 16 + i);
				result[k++]= proposal;
			}
		}		
		return result;
	}
	
	public JavaCompletionProposal[] getKeywordCompletions() {
		return (JavaCompletionProposal[]) fKeywords.toArray(new JavaCompletionProposal[fKeywords.size()]);
	}

	protected final int getLength(int start, int end) {
		int length;
		if (fUserReplacementLength == -1) {
			length= fPreventEating ? fCodeAssistOffset - start : end - start;
		} else {
			length= fUserReplacementLength;
			// extend length to begin at start
			if (start < fCodeAssistOffset) {
				length+= fCodeAssistOffset - start;
			}
		}
		return length;
	}
	
	protected final Image getImage(ImageDescriptor descriptor) {
		return (descriptor == null) ? null : fRegistry.get(descriptor);
	}
	
	private void acceptAnnotationAttributeReference(CompletionProposal proposal) {
		IJavaCompletionProposal uiProposal= createAnnotationAttributeReferenceProposal(proposal);
		if (uiProposal != null)
			fMethods.add(uiProposal);
	}
	protected IJavaCompletionProposal createAnnotationAttributeReferenceProposal(CompletionProposal proposal) {
		String displayString= fLabelProvider.createLabel(proposal);
		ImageDescriptor descriptor= fLabelProvider.createImageDescriptor(proposal);
		String completion= String.valueOf(proposal.getCompletion());
		return new JavaCompletionProposal(completion, proposal.getReplaceStart(), getLength(proposal.getReplaceStart(), proposal.getReplaceEnd()), getImage(descriptor), displayString, proposal.getRelevance(), fTextViewer);
	}
	
	/**
	 * Specifies the context of the code assist operation.
	 * @param codeAssistOffset The Offset at which the code assist will be called.
	 * Used to modify the offsets of the created proposals. ('Non Eating')
	 * @param contextOffset The offset at which the context presumable start or -1.
	 * @param jproject The Java project to which the underlying source belongs.
	 * Needed to find types referred.
	 * @param cu The compilation unit that is edited. Used to add import statements.
	 * Can be <code>null</code> if no import statements should be added.
	 */
	public void reset(int codeAssistOffset, int contextOffset, IJavaProject jproject, ICompilationUnit cu) {
		fJavaProject= jproject;
		fCompilationUnit= cu;
		fCodeAssistOffset= codeAssistOffset;
		fContextOffset= contextOffset;
		
		fUserReplacementLength= -1;
		
		fLastProblem= null;
		
		for (int i= 0; i < fResults.length; i++)
			fResults[i].clear();
		
		fSuggestedMethodNames.clear();
	}

	/**
	 * Specifies the context of the code assist operation.
	 * @param codeAssistOffset The Offset on which the code assist will be called.
	 * Used to modify the offsets of the created proposals. ('Non Eating')
	 * @param jproject The Java project to which the underlying source belongs.
	 * Needed to find types referred.
	 * @param cu The compilation unit that is edited. Used to add import statements.
	 * Can be <code>null</code> if no import statements should be added.
	 */
	public void reset(int codeAssistOffset, IJavaProject jproject, ICompilationUnit cu) {
		reset(codeAssistOffset, -1, jproject, cu);
	}
	
	/**
	 * Sets the text viewer.
	 */
	public void setViewer(ITextViewer viewer) {
		fTextViewer= viewer;
	}
	
	/**
	 * If the replacement length is set, it overrides the length returned from
	 * the content assist infrastructure.
	 * Use this setting if code assist is called with a none empty selection.
	 */
	public void setReplacementLength(int length) {
		fUserReplacementLength= length;
	}

	/**
	 * If set, proposals created will not remove characters after the code assist position
	 * @param preventEating The preventEating to set
	 */
	public void setPreventEating(boolean preventEating) {
		fPreventEating= preventEating;
	}
	
//	public void acceptContext(CompletionContext context) {
//		fContext= context;
//	}

	/* copied from CompletionRequestorWrapper */
	public void accept(CompletionProposal proposal) {
		long start= DEBUG ? System.currentTimeMillis() : 0;
		if (isIgnored(proposal.getKind()) || isFiltered(proposal))
			return;
		
		try {
			switch (proposal.getKind()) {
				case CompletionProposal.KEYWORD:
					acceptKeyword(proposal);
					break;
				case CompletionProposal.PACKAGE_REF:
					acceptPackage(proposal);
					break;
				case CompletionProposal.TYPE_REF:
					acceptType(proposal);
					break;
				case CompletionProposal.FIELD_REF:
					acceptField(proposal);
					break;
				case CompletionProposal.METHOD_REF:
				case CompletionProposal.METHOD_NAME_REFERENCE:
					acceptMethodReference(proposal);
					break;
				case CompletionProposal.METHOD_DECLARATION:
					acceptMethodDeclaration(proposal);
					break;
				case CompletionProposal.ANONYMOUS_CLASS_DECLARATION:
					acceptAnonymousType(proposal);
					break;
				case CompletionProposal.LABEL_REF:
					acceptLabel(proposal);
					break;
				case CompletionProposal.LOCAL_VARIABLE_REF:
				case CompletionProposal.VARIABLE_DECLARATION:
					acceptLocalVariable(proposal);
					break;
				case CompletionProposal.POTENTIAL_METHOD_DECLARATION:
					acceptPotentialMethodDeclaration(proposal);
					break;
				case CompletionProposal.ANNOTATION_ATTRIBUTE_REF:
					acceptAnnotationAttributeReference(proposal);
					break;
			}
		} catch (IllegalArgumentException e) {
			// all signature processing method may throw IAEs
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=84657
			// don't abort, but log and show all the valid proposals
			JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, "Exception when processing proposal for: " + String.valueOf(proposal.getCompletion()), e)); //$NON-NLS-1$
		}
		
		if (DEBUG) fUITime += System.currentTimeMillis() - start;
	}	

	/*
	 * @see org.eclipse.jdt.core.CompletionRequestor#beginReporting()
	 */
	public void beginReporting() {
		if (DEBUG) {
			fStartTime= System.currentTimeMillis();
			fUITime= 0;
		}
	}
	
	/*
	 * @see org.eclipse.jdt.core.CompletionRequestor#endReporting()
	 */
	public void endReporting() {
		if (DEBUG) {
			long total= System.currentTimeMillis() - fStartTime;
			System.err.println("Code Assist (core): " + (total - fUITime)); //$NON-NLS-1$
			System.err.println("Code Assist (UI): " + fUITime); //$NON-NLS-1$
		}
//		fContext= null;
	}
	
	/*
	 * @see org.eclipse.jdt.core.CompletionRequestor#completionFailure(org.eclipse.jdt.core.compiler.IProblem)
	 */
	public void completionFailure(IProblem problem) {
		fLastProblem= problem;
	}

	/**
	 * Returns <code>true</code> if generic proposals should be allowed,
	 * <code>false</code> if not. Note that even though code (in a library)
	 * may be referenced that uses generics, it is still possible that the
	 * current source does not allow generics.
	 * 
	 * @return <code>true</code> if the generic proposals should be allowed,
	 *         <code>false</code> if not
	 * @since 3.1
	 */
	protected boolean proposeGenerics() {
		String sourceVersion;
		if (fJavaProject != null)
			sourceVersion= fJavaProject.getOption(JavaCore.COMPILER_SOURCE, true);
		else
			sourceVersion= JavaCore.getOption(JavaCore.COMPILER_SOURCE);
	
		return sourceVersion != null && JavaCore.VERSION_1_5.compareTo(sourceVersion) <= 0; 
	}
	
	protected final int getCodeAssistOffset() {
		return fCodeAssistOffset;
	}
	protected final ICompilationUnit getCompilationUnit() {
		return fCompilationUnit;
	}
	protected final int getContextOffset() {
		return fContextOffset;
	}
	protected final IJavaProject getJavaProject() {
		return fJavaProject;
	}
	protected final ITextViewer getTextViewer() {
		return fTextViewer;
	}
	
	protected final ProposalLabelProvider getLabelProvider() {
		return fLabelProvider;
	}
	
	protected boolean isFiltered(CompletionProposal proposal) {
		char[] declaringType= getDeclaringType(proposal);
		return declaringType != null && TypeFilter.isFiltered(declaringType);
	}
	
	protected final char[] getDeclaringType(CompletionProposal proposal) {
		switch (proposal.getKind()) {
			case CompletionProposal.METHOD_DECLARATION:
			case CompletionProposal.METHOD_NAME_REFERENCE:
			case CompletionProposal.METHOD_REF:
			case CompletionProposal.ANNOTATION_ATTRIBUTE_REF:
			case CompletionProposal.POTENTIAL_METHOD_DECLARATION:
			case CompletionProposal.ANONYMOUS_CLASS_DECLARATION:
			case CompletionProposal.FIELD_REF:
				char[] declaration= proposal.getDeclarationSignature();
				// special methods may not have a declaring type: methods defined on arrays etc.
				// TODO remove when bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=84690 gets fixed
				if (declaration == null)
					return "java.lang.Object".toCharArray(); //$NON-NLS-1$
				return Signature.toCharArray(declaration);
			case CompletionProposal.PACKAGE_REF:
				return proposal.getDeclarationSignature();
			case CompletionProposal.TYPE_REF:
				char[] signature= proposal.getSignature();
				return Signature.toCharArray(signature);
			case CompletionProposal.LOCAL_VARIABLE_REF:
			case CompletionProposal.VARIABLE_DECLARATION:
			case CompletionProposal.KEYWORD:
			case CompletionProposal.LABEL_REF:
				return null;
			default:
				Assert.isTrue(false);
				return null;
		}
	}
	
}
