/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;

  
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jface.text.ITextViewer;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ICompletionRequestor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.internal.codeassist.IExtendedCompletionRequestor;

import org.eclipse.jdt.internal.corext.template.java.SignatureUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.TypeFilter;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

/**
 * Bin to collect the proposal of the infrastructure on code assist in a java text.
 */
public class ResultCollector extends CompletionRequestor implements ICompletionRequestor, IExtendedCompletionRequestor {
	
	private final static char[] METHOD_WITH_ARGUMENTS_TRIGGERS= new char[] { '(', '-', ' ' };
	private final static char[] METHOD_TRIGGERS= new char[] { ';', ',', '.', '\t', '[', ' ' };
	private final static char[] TYPE_TRIGGERS= new char[] { '.', '\t', '[', '(', ' ' };
	private final static char[] VAR_TRIGGER= new char[] { '\t', ' ', '=', ';', '.' };

	protected IJavaProject fJavaProject;
	protected ICompilationUnit fCompilationUnit; // set when imports can be added
	protected int fCodeAssistOffset;
	protected int fContextOffset;
	protected ITextViewer fTextViewer;
	
	private ArrayList fFields=
		new ArrayList(),
		fKeywords= new ArrayList(10), 
		fLabels= new ArrayList(10),
		fMethods= new ArrayList(),
		fModifiers= new ArrayList(10),
		fPackages= new ArrayList(),
		fTypes= new ArrayList(),
		fVariables= new ArrayList();

	private IProblem fLastProblem;	
	private ImageDescriptorRegistry fRegistry= JavaPlugin.getImageDescriptorRegistry();
	
	private ArrayList[] fResults = new ArrayList[] {
		fPackages, fLabels, fModifiers, fKeywords, fTypes, fMethods, fFields, fVariables
	};
	
	private Set fSuggestedMethodNames= new HashSet();
	
	private int fUserReplacementLength;

	/*
	 * Is eating code assist enabled or disabled? PR #3666
	 * When eating is enabled, JavaCompletionProposal must be revisited: PR #5533
	 */
	private boolean fPreventEating= true;	
	
	/*
	 * @see ICompletionRequestor#acceptClass
	 */	
	protected void internalAcceptType(char[] packageName, char[] typeName, char[] completionName, int modifiers, int start, int end, int relevance) {
		if (TypeFilter.isFiltered(packageName, typeName)) {
			return;
		}
		
		ImageDescriptor descriptor= JavaElementImageProvider.getTypeImageDescriptor(false, false, modifiers, false);
		if (Flags.isDeprecated(modifiers))
			descriptor= getDeprecatedDescriptor(descriptor);

		ProposalInfo info= new ProposalInfo(fJavaProject, packageName, typeName);
		fTypes.add(createTypeCompletion(start, end, new String(completionName), descriptor, new String(typeName), new String(packageName), info, relevance));
	}
	
	/*
	 * @see ICompletionRequestor#acceptClass
	 */	
	protected void internalAcceptType(CompletionProposal typeProposal) {
		char[] signature= typeProposal.getSignature();
		char[] packageName= Signature.getSignatureQualifier(signature); 
		char[] typeName= Signature.getSignatureSimpleName(signature);
		if (TypeFilter.isFiltered(packageName, typeName)) {
			return;
		}
		
		fTypes.add(createTypeCompletion(typeProposal));
	}
	
	/*
	 * @see ICompletionRequestor#acceptField
	 */	
	protected void internalAcceptField(
		char[] declaringTypePackageName, char[] declaringTypeName, char[] name,
		char[] typePackageName, char[] typeName, char[] completionName,
		int modifiers, int start, int end, int relevance) {
		
		if (TypeFilter.isFiltered(declaringTypePackageName, declaringTypeName)) {
			return;
		}
		

		ImageDescriptor descriptor= getFieldDescriptor(modifiers);	
		
		StringBuffer nameBuffer= new StringBuffer();
		nameBuffer.append(name);
		if (typeName.length > 0) {
			nameBuffer.append("   "); //$NON-NLS-1$
			nameBuffer.append(typeName);
		}
		if (declaringTypeName != null && declaringTypeName.length > 0) {
			nameBuffer.append(" - "); //$NON-NLS-1$
			nameBuffer.append(declaringTypeName);
		}	
		
		JavaCompletionProposal proposal= createCompletion(start, end, new String(completionName), descriptor, nameBuffer.toString(), relevance);
		proposal.setProposalInfo(new ProposalInfo(fJavaProject, declaringTypePackageName, declaringTypeName, name));
		proposal.setTriggerCharacters(VAR_TRIGGER);
		
		fFields.add(proposal);
	}
	
	/*
	 * @see ICompletionRequestor#acceptAnonymousType
	 */
	protected void internalAcceptAnonymousType(char[] superTypePackageName, char[] superTypeName, char[][] parameterPackageNames, char[][] parameterTypeNames, char[][] parameterNames,
			char[] completionName, int modifiers, int completionStart, int completionEnd, int relevance) {

		if (TypeFilter.isFiltered(superTypePackageName, superTypeName)) {
			return;
		}
		
		JavaCompletionProposal	proposal= createAnonymousTypeCompletion(superTypePackageName, superTypeName, parameterTypeNames, parameterNames, completionName, completionStart, completionEnd, relevance);
		proposal.setProposalInfo(new ProposalInfo(fJavaProject, superTypePackageName, superTypeName));
		fTypes.add(proposal);
	}	
	
	/*
	 * @see ICompletionRequestor#acceptKeyword
	 */	
	protected void internalAcceptKeyword(char[] keyword, int start, int end, int relevance) {
		String kw= new String(keyword);
		fKeywords.add(createCompletion(start, end, kw, null, kw, relevance));
	}
	
	/*
	 * @see ICompletionRequestor#acceptLabel
	 */	
	protected void internalAcceptLabel(char[] labelName, int start, int end, int relevance) {
		String ln= new String(labelName);
		fLabels.add(createCompletion(start, end, ln, null, ln, relevance));
	}
	
	/*
	 * @see ICompletionRequestor#acceptLocalVariable
	 */	
	protected void internalAcceptLocalVariable(char[] name, char[] typePackageName, char[] typeName, int modifiers, int start, int end, int relevance) {
		StringBuffer buf= new StringBuffer();
		buf.append(name);
		if (typeName != null) {
			buf.append("    "); //$NON-NLS-1$
			buf.append(typeName);
		}
		JavaCompletionProposal proposal= createCompletion(start, end, new String(name), JavaPluginImages.DESC_OBJS_LOCAL_VARIABLE, buf.toString(), relevance);
		proposal.setTriggerCharacters(VAR_TRIGGER);
		fVariables.add(proposal);
	}
	
	protected String getParameterSignature(char[][] parameterTypeNames, char[][] parameterNames) {
		StringBuffer buf = new StringBuffer();
		if (parameterTypeNames != null) {
			for (int i = 0; i < parameterTypeNames.length; i++) {
				if (i > 0) {
					buf.append(',');
					buf.append(' ');
				}
				buf.append(parameterTypeNames[i]);
				if (parameterNames != null && parameterNames[i] != null) {
					buf.append(' ');
					buf.append(parameterNames[i]);
				}
			}
		}
		return buf.toString();
	}
	
	/*
	 * @see ICompletionRequestor#acceptMethod
	 */
	protected void internalAcceptMethod(char[] declaringTypePackageName, char[] declaringTypeName, char[] name,
			char[][] parameterPackageNames, char[][] parameterTypeNames, char[][] parameterNames,
			char[] returnTypePackageName, char[] returnTypeName, char[] completionName, int modifiers,
			int start, int end, int relevance) {
		
		if (completionName == null)
			return;
		
		if (TypeFilter.isFiltered(declaringTypePackageName, declaringTypeName)) {
			return;
		}
	
		JavaCompletionProposal proposal= createMethodCallCompletion(declaringTypeName, name, parameterPackageNames, parameterTypeNames, parameterNames, returnTypeName, completionName, modifiers, start, end, relevance);
		boolean isConstructor= returnTypeName == null ? true : returnTypeName.length == 0;
		proposal.setProposalInfo(new ProposalInfo(fJavaProject, declaringTypePackageName, declaringTypeName, name, parameterPackageNames, parameterTypeNames, isConstructor));

		boolean hasOpeningBracket= completionName.length == 0 || (completionName.length > 0 && completionName[completionName.length - 1] == ')');
	
		ProposalContextInformation contextInformation= null;
		if (hasOpeningBracket && parameterTypeNames.length > 0) {
			contextInformation= new ProposalContextInformation();
			contextInformation.setInformationDisplayString(getParameterSignature(parameterTypeNames, parameterNames));		
			contextInformation.setContextDisplayString(proposal.getDisplayString());
			contextInformation.setImage(proposal.getImage());
			int position= (completionName.length == 0) ? fContextOffset : -1;
			contextInformation.setContextInformationPosition(position);
			proposal.setContextInformation(contextInformation);
		}
	
		boolean userMustCompleteParameters= (contextInformation != null && completionName.length > 0);
		char[] triggers= userMustCompleteParameters ? METHOD_WITH_ARGUMENTS_TRIGGERS : METHOD_TRIGGERS;
		proposal.setTriggerCharacters(triggers);
		
		if (userMustCompleteParameters) {
			// set the cursor before the closing bracket
			proposal.setCursorPosition(completionName.length - 1);
		}
		
		fMethods.add(proposal);	
	}
	
	protected void internalAcceptMethod(CompletionProposal method) {
		char[] declaringTypePackageName= Signature.getSignatureQualifier(method.getDeclarationSignature());
		char[] declaringTypeName= Signature.getSignatureSimpleName(method.getDeclarationSignature());
		char[] name= method.getName();
		char[] signature= SignatureUtil.unboundedSignature(method.getSignature());
		char[][] parameterPackageNames= getParameterPackages(signature);
		char[][] parameterTypeNames= getParameterTypes(signature);
		char[][] parameterNames= method.findParameterNames(null) == null ? CharOperation.NO_CHAR_CHAR : method.findParameterNames(null);
		char[] returnTypeName= Signature.getSignatureSimpleName(Signature.getReturnType(signature));
		char[] completionName= method.getCompletion();
		int modifiers= method.getFlags();
		int start= method.getReplaceStart();
		int end= method.getReplaceEnd();
		int relevance= method.getRelevance();

		if (completionName == null)
			return;
		
		if (TypeFilter.isFiltered(declaringTypePackageName, declaringTypeName)) {
			return;
		}
		
		JavaCompletionProposal proposal= createMethodCallCompletion(declaringTypeName, name, parameterPackageNames, parameterTypeNames, parameterNames, returnTypeName, completionName, modifiers, start, end, relevance);
		boolean isConstructor= returnTypeName == null || returnTypeName.length == 0;
		proposal.setProposalInfo(new ProposalInfo(fJavaProject, declaringTypePackageName, declaringTypeName, name, parameterPackageNames, parameterTypeNames, isConstructor));

		boolean hasOpeningBracket= completionName.length == 0 || (completionName.length > 0 && completionName[completionName.length - 1] == ')');
	
		ProposalContextInformation contextInformation= null;
		if (hasOpeningBracket && parameterTypeNames.length > 0) {
			contextInformation= new ProposalContextInformation();
			contextInformation.setInformationDisplayString(getParameterSignature(parameterTypeNames, parameterNames));		
			contextInformation.setContextDisplayString(proposal.getDisplayString());
			contextInformation.setImage(proposal.getImage());
			int position= (completionName.length == 0) ? fContextOffset : -1;
			contextInformation.setContextInformationPosition(position);
			proposal.setContextInformation(contextInformation);
		}
	
		boolean userMustCompleteParameters= (contextInformation != null && completionName.length > 0);
		char[] triggers= userMustCompleteParameters ? METHOD_WITH_ARGUMENTS_TRIGGERS : METHOD_TRIGGERS;
		proposal.setTriggerCharacters(triggers);
		
		if (userMustCompleteParameters) {
			// set the cursor before the closing bracket
			proposal.setCursorPosition(completionName.length - 1);
		}
		
		fMethods.add(proposal);	
	}
		
	/*
	 * @see ICompletionRequestor#acceptModifier
	 */	
	protected void internalAcceptModifier(char[] modifier, int start, int end, int relevance) {
		String mod= new String(modifier);
		fModifiers.add(createCompletion(start, end, mod, null, mod, relevance));
	}
	
	/*
	 * @see ICompletionRequestor#acceptPackage
	 */	
	protected void internalAcceptPackage(char[] packageName, char[] completionName, int start, int end, int relevance) {
		if (TypeFilter.isFiltered(new String(packageName))) {
			return;
		}
		
		fPackages.add(createCompletion(start, end, new String(completionName), JavaPluginImages.DESC_OBJS_PACKAGE, new String(packageName), relevance));
	}
	
	/*
	 * @see ICodeCompletionRequestor#acceptMethodDeclaration
	 */
	protected void internalAcceptMethodDeclaration(char[] declaringTypePackageName, char[] declaringTypeName, char[] name, char[][] parameterPackageNames, char[][] parameterTypeNames, char[][] parameterNames, char[] returnTypePackageName, char[] returnTypeName, char[] completionName, int modifiers, int start, int end, int relevance) {
		StringBuffer displayString= getMethodDisplayString(null, name, parameterTypeNames, parameterNames, returnTypeName);
		displayString.append(" - "); //$NON-NLS-1$
		displayString.append(JavaTextMessages.getFormattedString("ResultCollector.overridingmethod", new String(declaringTypeName))); //$NON-NLS-1$
		
		StringBuffer typeName= new StringBuffer();
		if (declaringTypePackageName.length > 0) {
			typeName.append(declaringTypePackageName);
			typeName.append('.');
		}
		typeName.append(declaringTypeName);
		
		String[] paramTypes= new String[parameterTypeNames.length];
		for (int i= 0; i < parameterTypeNames.length; i++) {
			paramTypes[i]= Signature.createTypeSignature(parameterTypeNames[i], true);
		}

		JavaCompletionProposal proposal= new OverrideCompletionProposal(fJavaProject, fCompilationUnit, typeName.toString(), new String(name), paramTypes, start, getLength(start, end), displayString.toString(), new String(completionName));
		proposal.setImage(getImage(getMemberDescriptor(modifiers)));
		proposal.setProposalInfo(new ProposalInfo(fJavaProject, declaringTypePackageName, declaringTypeName, name, parameterPackageNames, parameterTypeNames, returnTypeName.length == 0));
		proposal.setRelevance(relevance + 100);
		fMethods.add(proposal);
		
		fSuggestedMethodNames.add(new String(name));
		
	}
	
	/*
	 * @see IExtendedCompletionRequestor#acceptPotentialMethodDeclaration
	 */
	protected void internalAcceptPotentialMethodDeclaration(char[] declaringTypePackageName, char[] declaringTypeName, char[] selector, int completionStart, int completionEnd, int relevance) {
		if (fCompilationUnit == null) {
			return;
		}
		String prefix= new String(selector);
	
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
	
	/*
	 * @see ICodeCompletionRequestor#acceptVariableName
	 */
	protected void internalAcceptVariableName(char[] typePackageName, char[] typeName, char[] name, char[] completionName, int start, int end, int relevance) {
		// XXX: To be revised
		StringBuffer buf= new StringBuffer();
		buf.append(name);
		if (typeName != null && typeName.length > 0) {
			buf.append(" - "); //$NON-NLS-1$
			buf.append(typeName);
		}
		JavaCompletionProposal proposal= createCompletion(start, end, new String(completionName), null, buf.toString(), relevance);
		proposal.setTriggerCharacters(VAR_TRIGGER);
		fVariables.add(proposal);
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
			ArrayList curr= fResults[i];
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

	protected StringBuffer getMethodDisplayString(char[] declaringTypeName, char[] name, char[][] parameterTypeNames, char[][] parameterNames, char[] returnTypeName) {
		StringBuffer nameBuffer= new StringBuffer();
		nameBuffer.append(name);
		nameBuffer.append('(');
		if (parameterTypeNames != null && parameterTypeNames.length > 0) {
			nameBuffer.append(getParameterSignature(parameterTypeNames, parameterNames));
		}
		nameBuffer.append(')'); 
		if (returnTypeName != null && returnTypeName.length > 0) {
			nameBuffer.append("  "); //$NON-NLS-1$
			nameBuffer.append(returnTypeName);
		}
		if (declaringTypeName != null && declaringTypeName.length > 0) {
			nameBuffer.append(" - "); //$NON-NLS-1$
			nameBuffer.append(declaringTypeName);
		}
		return nameBuffer;
	}

	protected JavaCompletionProposal createMethodCallCompletion(char[] declaringTypeName, char[] name, char[][] parameterTypePackageNames, char[][] parameterTypeNames, char[][] parameterNames, char[] returnTypeName, char[] completionName, int modifiers, int start, int end, int relevance) {
		ImageDescriptor descriptor= getMemberDescriptor(modifiers);
		StringBuffer nameBuffer= getMethodDisplayString(declaringTypeName, name, parameterTypeNames, parameterNames, returnTypeName);
		return createCompletion(start, end, new String(completionName), descriptor, nameBuffer.toString(), relevance);
	}


	protected JavaCompletionProposal createAnonymousTypeCompletion(char[] declaringTypePackageName, char[] declaringTypeName, char[][] parameterTypeNames, char[][] parameterNames, char[] completionName, int start, int end, int relevance) {
		StringBuffer declTypeBuf= new StringBuffer();
		if (declaringTypePackageName.length > 0) {
			declTypeBuf.append(declaringTypePackageName);
			declTypeBuf.append('.');
		}
		declTypeBuf.append(declaringTypeName);
		
		StringBuffer nameBuffer= new StringBuffer();
		nameBuffer.append(declaringTypeName);
		nameBuffer.append('(');
		if (parameterTypeNames.length > 0) {
			nameBuffer.append(getParameterSignature(parameterTypeNames, parameterNames));
		}
		nameBuffer.append(')');
		nameBuffer.append("  "); //$NON-NLS-1$
		nameBuffer.append(JavaTextMessages.getString("ResultCollector.anonymous_type")); //$NON-NLS-1$
	
		int length= end - start;
		
		return new AnonymousTypeCompletionProposal(fJavaProject, fCompilationUnit, start, length, new String(completionName), nameBuffer.toString(), declTypeBuf.toString(), relevance);
	}

	protected JavaCompletionProposal createTypeCompletion(CompletionProposal typeProposal) {
		char[] signature= typeProposal.getSignature();
		char[] packageName= Signature.getSignatureQualifier(signature); 
		char[] typeName= Signature.getSignatureSimpleName(signature);
		
		String completion= String.valueOf(typeProposal.getCompletion());
		int modifiers= typeProposal.getFlags();
		int start= typeProposal.getReplaceStart();
		int end= typeProposal.getReplaceEnd();
		int relevance= typeProposal.getRelevance();
		
		ImageDescriptor descriptor= JavaElementImageProvider.getTypeImageDescriptor(false, false, modifiers, false);
		if (Flags.isDeprecated(modifiers))
			descriptor= getDeprecatedDescriptor(descriptor);

		ProposalInfo info= new ProposalInfo(fJavaProject, packageName, typeName);
		
		StringBuffer buf= new StringBuffer();
		buf.append(typeName);
		if (packageName.length > 0) {
			buf.append(" - "); //$NON-NLS-1$
			buf.append(packageName);
		}
		String name= buf.toString();

		JavaCompletionProposal proposal= new JavaTypeCompletionProposal(completion, fCompilationUnit, start, getLength(start, end), getImage(descriptor), name, relevance, String.valueOf(typeName), String.valueOf(packageName));
		proposal.setProposalInfo(info);
		proposal.setTriggerCharacters(TYPE_TRIGGERS);
		return proposal;
	}
	
	
	protected JavaCompletionProposal createTypeCompletion(int start, int end, String completion, ImageDescriptor descriptor, String typeName, String containerName, ProposalInfo proposalInfo, int relevance) {
		
		String fullName= JavaModelUtil.concatenateName(containerName, typeName); // containername can be null
		
		StringBuffer buf= new StringBuffer(Signature.getSimpleName(fullName));
		String typeQualifier= Signature.getQualifier(fullName);
		if (typeQualifier.length() > 0) {
			buf.append(" - "); //$NON-NLS-1$
			buf.append(typeQualifier);
		}
		String name= buf.toString();

		ICompilationUnit cu= null;
		if (containerName != null && fCompilationUnit != null) {
			if (completion.equals(fullName)) {
				cu= fCompilationUnit;
			}
		}
	
		JavaCompletionProposal proposal= new JavaTypeCompletionProposal(completion, cu, start, getLength(start, end), getImage(descriptor), name, relevance, typeName, containerName);
		proposal.setProposalInfo(proposalInfo);
		proposal.setTriggerCharacters(TYPE_TRIGGERS);
		return proposal;
	}

	protected ImageDescriptor getMemberDescriptor(int modifiers) {
		ImageDescriptor desc= JavaElementImageProvider.getMethodImageDescriptor(false, modifiers);

		if (Flags.isDeprecated(modifiers))
		 	desc= getDeprecatedDescriptor(desc);

		if (Flags.isStatic(modifiers))
			desc= getStaticDescriptor(desc);
		
		return desc;
	}
	
	protected ImageDescriptor getFieldDescriptor(int modifiers) {
		ImageDescriptor desc= JavaElementImageProvider.getFieldImageDescriptor(false, modifiers);

		if (Flags.isDeprecated(modifiers))
		 	desc= getDeprecatedDescriptor(desc);
		 	
		if (Flags.isStatic(modifiers))
			desc= getStaticDescriptor(desc);
		
		return desc;
	}	
	
	protected ImageDescriptor getDeprecatedDescriptor(ImageDescriptor descriptor) {
		return new JavaElementImageDescriptor(descriptor, JavaElementImageDescriptor.DEPRECATED, JavaElementImageProvider.SMALL_SIZE);	    
	}
	
	protected ImageDescriptor getStaticDescriptor(ImageDescriptor descriptor) {
		return new JavaElementImageDescriptor(descriptor, JavaElementImageDescriptor.STATIC, JavaElementImageProvider.SMALL_SIZE);
	}
	
	protected JavaCompletionProposal createCompletion(int start, int end, String completion, ImageDescriptor descriptor, String name, int relevance) {
		return new JavaCompletionProposal(completion, start, getLength(start, end), getImage(descriptor), name, relevance, fTextViewer);
	}

	private int getLength(int start, int end) {
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
	
	protected Image getImage(ImageDescriptor descriptor) {
		return (descriptor == null) ? null : fRegistry.get(descriptor);
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

	/* copied from CompletionRequestorWrapper */
	public void accept(CompletionProposal proposal) {
		switch(proposal.getKind()) {
			case CompletionProposal.KEYWORD:
				internalAcceptKeyword(
						proposal.getName(),
						proposal.getReplaceStart(),
						proposal.getReplaceEnd(),
						proposal.getRelevance());
				break;
			case CompletionProposal.PACKAGE_REF:
				internalAcceptPackage(
						proposal.getDeclarationSignature(),
						proposal.getCompletion(),
						proposal.getReplaceStart(),
						proposal.getReplaceEnd(),
						proposal.getRelevance());
				break;
			case CompletionProposal.TYPE_REF:
				internalAcceptType(proposal);
				break;
			case CompletionProposal.FIELD_REF:
				char[] signatureSimpleName= Signature.getSignatureSimpleName(proposal.getSignature());
				if (signatureSimpleName == null) signatureSimpleName= CharOperation.NO_CHAR;
				internalAcceptField(
						Signature.getSignatureQualifier(proposal.getDeclarationSignature()),
						Signature.getSignatureSimpleName(proposal.getDeclarationSignature()),
						proposal.getName(),
						Signature.getSignatureQualifier(proposal.getSignature()),
						signatureSimpleName, 
						proposal.getCompletion(),
						proposal.getFlags(),
						proposal.getReplaceStart(),
						proposal.getReplaceEnd(),
						proposal.getRelevance()
				);
				break;
			case CompletionProposal.METHOD_REF:
				internalAcceptMethod(proposal);
				break;
			case CompletionProposal.METHOD_DECLARATION:
				internalAcceptMethodDeclaration(
						Signature.getSignatureQualifier(proposal.getDeclarationSignature()),
						Signature.getSignatureSimpleName(proposal.getDeclarationSignature()),
						proposal.getName(),
						getParameterPackages(proposal.getSignature()),
						getParameterTypes(proposal.getSignature()),
						proposal.findParameterNames(null) == null ? CharOperation.NO_CHAR_CHAR : proposal.findParameterNames(null),
						Signature.getSignatureQualifier(Signature.getReturnType(proposal.getSignature())),
						Signature.getSignatureSimpleName(Signature.getReturnType(proposal.getSignature())),
						proposal.getCompletion(),
						proposal.getFlags(),
						proposal.getReplaceStart(),
						proposal.getReplaceEnd(),
						proposal.getRelevance()
				);
				break;
			case CompletionProposal.ANONYMOUS_CLASS_DECLARATION:
				internalAcceptAnonymousType(
						Signature.getSignatureQualifier(proposal.getDeclarationSignature()),
						Signature.getSignatureSimpleName(proposal.getDeclarationSignature()), 
						getParameterPackages(proposal.getSignature()),
						getParameterTypes(proposal.getSignature()),
						proposal.findParameterNames(null) == null ? CharOperation.NO_CHAR_CHAR : proposal.findParameterNames(null),
						proposal.getCompletion(),
						proposal.getFlags(),
						proposal.getReplaceStart(),
						proposal.getReplaceEnd(),
						proposal.getRelevance()
				);
				break;
			case CompletionProposal.LABEL_REF :
				internalAcceptLabel(
						proposal.getCompletion(),
						proposal.getReplaceStart(),
						proposal.getReplaceEnd(),
						proposal.getRelevance()
				);
				break;
			case CompletionProposal.LOCAL_VARIABLE_REF:
				signatureSimpleName= Signature.getSignatureSimpleName(proposal.getSignature());
				if (signatureSimpleName == null) signatureSimpleName= CharOperation.NO_CHAR;
				internalAcceptLocalVariable(
						proposal.getCompletion(),
						Signature.getSignatureQualifier(proposal.getSignature()),
						signatureSimpleName,
						proposal.getFlags(),
						proposal.getReplaceStart(),
						proposal.getReplaceEnd(),
						proposal.getRelevance()
				);
				break;
			case CompletionProposal.VARIABLE_DECLARATION:
				signatureSimpleName= Signature.getSignatureSimpleName(proposal.getSignature());
				if (signatureSimpleName == null) signatureSimpleName= CharOperation.NO_CHAR;
				internalAcceptLocalVariable(
						proposal.getCompletion(),
						Signature.getSignatureQualifier(proposal.getSignature()),
						signatureSimpleName,
						proposal.getFlags(),
						proposal.getReplaceStart(),
						proposal.getReplaceEnd(),
						proposal.getRelevance()
				);
				break;
			case CompletionProposal.POTENTIAL_METHOD_DECLARATION:
				internalAcceptPotentialMethodDeclaration(
						Signature.getSignatureQualifier(proposal.getDeclarationSignature()),
						Signature.getSignatureSimpleName(proposal.getDeclarationSignature()),
						proposal.getName(),
						proposal.getReplaceStart(),
						proposal.getReplaceEnd(),
						proposal.getRelevance()
				);
				break;
				
		}
	}	
	
	private char[][] getParameterPackages(char[] methodSignature) {
		char[][] parameterQualifiedTypes = Signature.getParameterTypes(methodSignature);
		int length = parameterQualifiedTypes == null ? 0 : parameterQualifiedTypes.length;
		char[][] parameterPackages = new char[length][];
		for(int i = 0; i < length; i++) {
			parameterPackages[i] = Signature.getSignatureQualifier(parameterQualifiedTypes[i]);
		}

		return parameterPackages;
	}
	
	private char[][] getParameterTypes(char[] methodSignature) {
		char[][] parameterQualifiedTypes = Signature.getParameterTypes(methodSignature);
		int length = parameterQualifiedTypes == null ? 0 : parameterQualifiedTypes.length;
		char[][] parameterTypes = new char[length][];
		for(int i = 0; i < length; i++) {
			parameterTypes[i] = Signature.getSignatureSimpleName(parameterQualifiedTypes[i]);
		}

		return parameterTypes;
	}

	/*
	 * @see org.eclipse.jdt.core.CompletionRequestor#beginReporting()
	 */
	public void beginReporting() {
	}
	
	/*
	 * @see org.eclipse.jdt.core.CompletionRequestor#endReporting()
	 */
	public void endReporting() {
	}
	
	/* 
	 * TODO remove once we're getting rid of ICompletionRequestor 
	 * these are just the delegating implementation now
	 */
	
	/*
	 * @see org.eclipse.jdt.core.CompletionRequestor#completionFailure(org.eclipse.jdt.core.compiler.IProblem)
	 */
	public void completionFailure(IProblem problem) {
		fLastProblem= problem;
	}
	
	/*
	 * @see org.eclipse.jdt.core.ICompletionRequestor#acceptAnonymousType(char[], char[], char[][], char[][], char[][], char[], int, int, int, int)
	 */
	public final void acceptAnonymousType(char[] superTypePackageName, char[] superTypeName, char[][] parameterPackageNames, char[][] parameterTypeNames, char[][] parameterNames, char[] completionName, int modifiers, int completionStart, int completionEnd, int relevance) {
		internalAcceptAnonymousType(superTypePackageName, superTypeName, parameterPackageNames, parameterTypeNames, parameterNames, completionName, modifiers, completionStart, completionEnd, relevance);
	}

	/*
	 * @see org.eclipse.jdt.core.ICompletionRequestor#acceptClass(char[], char[], char[], int, int, int, int)
	 */
	public final void acceptClass(char[] packageName, char[] className, char[] completionName, int modifiers, int completionStart, int completionEnd, int relevance) {
		internalAcceptType(packageName, className, completionName, modifiers, completionStart, completionEnd, relevance);
	}

	/*
	 * @see org.eclipse.jdt.core.ICompletionRequestor#acceptField(char[], char[], char[], char[], char[], char[], int, int, int, int)
	 */
	public final void acceptField(char[] declaringTypePackageName, char[] declaringTypeName, char[] name, char[] typePackageName, char[] typeName, char[] completionName, int modifiers, int completionStart, int completionEnd, int relevance) {
		internalAcceptField(declaringTypePackageName, declaringTypeName, name, typePackageName, typeName, completionName, modifiers, completionStart, completionEnd, relevance);
	}

	/*
	 * @see org.eclipse.jdt.core.ICompletionRequestor#acceptInterface(char[], char[], char[], int, int, int, int)
	 */
	public final void acceptInterface(char[] packageName, char[] interfaceName, char[] completionName, int modifiers, int completionStart, int completionEnd, int relevance) {
		internalAcceptInterface(packageName, interfaceName, completionName, modifiers, completionStart, completionEnd, relevance);
	}
	
	private void internalAcceptInterface(char[] packageName, char[] typeName, char[] completionName, int modifiers, int start, int end, int relevance) {
		internalAcceptType(packageName, typeName, completionName, modifiers | Flags.AccInterface, start, end, relevance);
	}
	
	/*
	 * @see org.eclipse.jdt.core.ICompletionRequestor#acceptKeyword(char[], int, int, int)
	 */
	public final void acceptKeyword(char[] keywordName, int completionStart, int completionEnd, int relevance) {
		internalAcceptKeyword(keywordName, completionStart, completionEnd, relevance);
	}

	/*
	 * @see org.eclipse.jdt.core.ICompletionRequestor#acceptLabel(char[], int, int, int)
	 */
	public final void acceptLabel(char[] labelName, int completionStart, int completionEnd, int relevance) {
		internalAcceptLabel(labelName, completionStart, completionEnd, relevance);
	}

	/*
	 * @see org.eclipse.jdt.core.ICompletionRequestor#acceptLocalVariable(char[], char[], char[], int, int, int, int)
	 */
	public final void acceptLocalVariable(char[] name, char[] typePackageName, char[] typeName, int modifiers, int completionStart, int completionEnd, int relevance) {
		internalAcceptLocalVariable(name, typePackageName, typeName, modifiers, completionStart, completionEnd, relevance);
	}

	/*
	 * @see org.eclipse.jdt.core.ICompletionRequestor#acceptMethod(char[], char[], char[], char[][], char[][], char[][], char[], char[], char[], int, int, int, int)
	 */
	public final void acceptMethod(char[] declaringTypePackageName, char[] declaringTypeName, char[] selector, char[][] parameterPackageNames, char[][] parameterTypeNames, char[][] parameterNames, char[] returnTypePackageName, char[] returnTypeName, char[] completionName, int modifiers, int completionStart, int completionEnd, int relevance) {
		internalAcceptMethod(declaringTypePackageName, declaringTypeName, selector, parameterPackageNames, parameterTypeNames, parameterNames, returnTypePackageName, returnTypeName, completionName, modifiers, completionStart, completionEnd, relevance);
	}

	/*
	 * @see org.eclipse.jdt.core.ICompletionRequestor#acceptMethodDeclaration(char[], char[], char[], char[][], char[][], char[][], char[], char[], char[], int, int, int, int)
	 */
	public final void acceptMethodDeclaration(char[] declaringTypePackageName, char[] declaringTypeName, char[] selector, char[][] parameterPackageNames, char[][] parameterTypeNames, char[][] parameterNames, char[] returnTypePackageName, char[] returnTypeName, char[] completionName, int modifiers, int completionStart, int completionEnd, int relevance) {
		internalAcceptMethodDeclaration(declaringTypePackageName, declaringTypeName, selector, parameterPackageNames, parameterTypeNames, parameterNames, returnTypePackageName, returnTypeName, completionName, modifiers, completionStart, completionEnd, relevance);
	}

	/*
	 * @see org.eclipse.jdt.core.ICompletionRequestor#acceptModifier(char[], int, int, int)
	 */
	public final void acceptModifier(char[] modifierName, int completionStart, int completionEnd, int relevance) {
		internalAcceptModifier(modifierName, completionStart, completionEnd, relevance);
	}

	/*
	 * @see org.eclipse.jdt.core.ICompletionRequestor#acceptPackage(char[], char[], int, int, int)
	 */
	public final void acceptPackage(char[] packageName, char[] completionName, int completionStart, int completionEnd, int relevance) {
		internalAcceptPackage(packageName, completionName, completionStart, completionEnd, relevance);
	}

	/*
	 * @see org.eclipse.jdt.core.ICompletionRequestor#acceptType(char[], char[], char[], int, int, int)
	 */
	public final void acceptType(char[] packageName, char[] typeName, char[] completionName, int completionStart, int completionEnd, int relevance) {
		internalAcceptType(packageName, typeName, completionName, completionStart, completionEnd, relevance);
	}

	private void internalAcceptType(char[] packageName, char[] typeName, char[] completionName, int start, int end, int relevance) {
		if (TypeFilter.isFiltered(packageName, typeName)) {
			return;
		}
		
		ProposalInfo info= new ProposalInfo(fJavaProject, packageName, typeName);
		fTypes.add(createTypeCompletion(start, end, new String(completionName), JavaPluginImages.DESC_OBJS_CLASS, new String(typeName), new String(packageName), info, relevance));
	}
	
	/*
	 * @see org.eclipse.jdt.core.ICompletionRequestor#acceptVariableName(char[], char[], char[], char[], int, int, int)
	 */
	public final void acceptVariableName(char[] typePackageName, char[] typeName, char[] name, char[] completionName, int completionStart, int completionEnd, int relevance) {
		internalAcceptVariableName(typePackageName, typeName, name, completionName, completionStart, completionEnd, relevance);
	}

	/*
	 * @see org.eclipse.jdt.internal.codeassist.IExtendedCompletionRequestor#acceptPotentialMethodDeclaration(char[], char[], char[], int, int, int)
	 */
	public final void acceptPotentialMethodDeclaration(char[] declaringTypePackageName, char[] declaringTypeName, char[] selector, int completionStart, int completionEnd, int relevance) {
		internalAcceptPotentialMethodDeclaration(declaringTypePackageName, declaringTypeName, selector, completionStart, completionEnd, relevance);
	}

	/*
	 * @see org.eclipse.jdt.core.ICompletionRequestor#acceptError(org.eclipse.jdt.core.compiler.IProblem)
	 */
	public final void acceptError(IProblem error) {
		completionFailure(error);
	}
}
