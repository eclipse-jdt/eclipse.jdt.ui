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
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jface.text.ITextViewer;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.corext.template.java.SignatureUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.TypeFilter;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

/**
 * Java implementation of <code>CompletionRequestor</code>.
 * 
 * @since 3.1
 */
public class ResultCollector extends CompletionRequestor {
	
	private final static char[] METHOD_WITH_ARGUMENTS_TRIGGERS= new char[] { '(', '-', ' ' };
	private final static char[] METHOD_TRIGGERS= new char[] { ';', ',', '.', '\t', '[', ' ' };
	private final static char[] TYPE_TRIGGERS= new char[] { '.', '\t', '[', '(', ' ' };
	private final static char[] VAR_TRIGGER= new char[] { '\t', ' ', '=', ';', '.' };

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

		if (fCompilationUnit == null || TypeFilter.isFiltered(superTypePackageName, superTypeName)) {
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
	
	/**
	 * Accepts a completion description and tries to create a proposal that can
	 * be displayed to the user.
	 *  
	 * @param methodProposal the completion proposal of type METHOD_REF.
	 */
	private final void acceptMethodProposal(CompletionProposal methodProposal) {
		IJavaCompletionProposal proposal= createMethodProposal(methodProposal);
		if (proposal != null)
			fMethods.add(proposal);
	}
	
	/**
	 * Creates a completion proposal (UI) given a completion proposal of type
	 * <code>CompletionProposal.METHOD_REF</code> (core).
	 * 
	 * @param methodProposal the method proposal to accept
	 * @return a newly created proposal, or <code>null</code> if none should
	 *         be added
	 */
	protected IJavaCompletionProposal createMethodProposal(CompletionProposal methodProposal) {
		String rawDeclaringType= extractTypeFQN(methodProposal);
		if (TypeFilter.isFiltered(rawDeclaringType))
			return null;
		
		String parameterList= createUnboundedParameterList(methodProposal);
		JavaCompletionProposal proposal= createMethodCallCompletion(methodProposal, parameterList);
		proposal.setProposalInfo(new MethodProposalInfo(fJavaProject, methodProposal));

		char[] completionName= methodProposal.getCompletion();
		boolean hasParameters= Signature.getParameterCount(methodProposal.getSignature()) > 0;
		if (hasParameters) {
			ProposalContextInformation contextInformation= createContextInformation(parameterList, proposal, completionName);
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

	private ProposalContextInformation createContextInformation(String parameterList, JavaCompletionProposal proposal, char[] completionName) {
		ProposalContextInformation contextInformation= new ProposalContextInformation();
		contextInformation.setInformationDisplayString(parameterList);
		contextInformation.setImage(proposal.getImage());
		contextInformation.setContextDisplayString(proposal.getDisplayString());
		contextInformation.setContextInformationPosition(completionName.length == 0 ? fContextOffset : -1);
		return contextInformation;
	}

	/**
	 * Extracts the fully qualified name of the declaring type of a method
	 * reference.
	 * 
	 * @param methodProposal the proposed method
	 * @return the qualified name of the declaring type
	 */
	private String extractTypeFQN(CompletionProposal methodProposal) {
		char[] declaringTypeSignature= methodProposal.getDeclarationSignature();
		// special methods may not have a declaring type: methods defined on arrays etc.
		// TODO remove when bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=84690 gets fixed
		if (declaringTypeSignature == null)
			return "java.lang.Object"; //$NON-NLS-1$
		return SignatureUtil.stripSignatureToFQN(String.valueOf(declaringTypeSignature));
	}
	
	/**
	 * Creates and returns a parameter list of the given method proposal
	 * suitable for display. The list does not include parentheses. The
	 * parameter types are filtered trough
	 * {@link SignatureUtil#getLowerBound(char[])}.
	 * 
	 * @param methodProposal the method proposal to create the parameter list
	 *        for
	 * @return the list of comma-separated parameters suitable for display
	 * @since 3.1
	 */
	protected final String createUnboundedParameterList(CompletionProposal methodProposal) {
		char[] signature= SignatureUtil.fix83600(methodProposal.getSignature());
		char[][] parameterNames= methodProposal.findParameterNames(null);
		char[][] parameterTypes= Signature.getParameterTypes(signature);
		for (int i= 0; i < parameterTypes.length; i++) {
			parameterTypes[i]= createTypeDisplayName(SignatureUtil.getLowerBound(parameterTypes[i]));
		}
		return getParameterSignature(parameterTypes, parameterNames);
	}

	/**
	 * Returns a visual representation of a java type passed in as type signature.
	 * 
	 * @param typeSignature the type signature to create a display name for
	 * @return the display name for <code>typeSignature</code>
	 */
	private char[] createTypeDisplayName(char[] typeSignature) {
		char[] displayName= Signature.getSimpleName(Signature.toCharArray(typeSignature));
		// XXX see https://bugs.eclipse.org/bugs/show_bug.cgi?id=84675
		boolean useShortGenerics= false;
		if (useShortGenerics) {
			StringBuffer buf= new StringBuffer();
			buf.append(displayName);
			int pos;
			do {
				pos= buf.indexOf("? extends "); //$NON-NLS-1$
				if (pos >= 0) {
					buf.replace(pos, pos + 10, "+"); //$NON-NLS-1$
				} else {
					pos= buf.indexOf("? super "); //$NON-NLS-1$
					if (pos >= 0)
						buf.replace(pos, pos + 8, "-"); //$NON-NLS-1$
				}
			} while (pos >= 0);
			return buf.toString().toCharArray();
		}
		return displayName;
	}

	/**
	 * Creates a display string of a parameter list (without the parentheses) for the given
	 * parameter types and names.
	 * 
	 * @param parameterTypes the parameter types
	 * @param parameterNames the parameter names
	 * @return the display string of the parameter list defined by the passed arguments
	 */
	private final String getParameterSignature(char[][] parameterTypes, char[][] parameterNames) {
		StringBuffer buf = new StringBuffer();
		if (parameterTypes != null) {
			for (int i = 0; i < parameterTypes.length; i++) {
				if (i > 0) {
					buf.append(',');
					buf.append(' ');
				}
				buf.append(parameterTypes[i]);
				if (parameterNames != null && parameterNames[i] != null) {
					buf.append(' ');
					buf.append(parameterNames[i]);
				}
			}
		}
		return buf.toString();
	}
	
	/**
	 * Creates an <code>IJavaCompletionProposal</code> for a proposed method reference.
	 * TODO remove reference to internal class
	 * @param methodProposal the proposal describing the method to be inserted
	 * @param parameterList the display string for the methods parameter list
	 * @return a completion proposal that describes how the proposal should be displayed to the user
	 */
	protected JavaCompletionProposal createMethodCallCompletion(CompletionProposal methodProposal, String parameterList) {
		ImageDescriptor descriptor= createMethodImageDescriptor(methodProposal.getFlags());
		String displayName= createMethodDisplayString(methodProposal, parameterList);
		String completion= String.valueOf(methodProposal.getCompletion());
		int start= methodProposal.getReplaceStart();
		int end= methodProposal.getReplaceEnd();
		int relevance= methodProposal.getRelevance();
		
		return createCompletion(start, end, completion, descriptor, displayName, relevance);
	}

	/**
	 * Creates and returns a decorated image descriptor for a method.
	 * 
	 * @param modifiers the modifiers, see {@link Flags}
	 * @return the created imaged descriptor
	 */
	protected final ImageDescriptor createMethodImageDescriptor(int modifiers) {
		ImageDescriptor desc= JavaElementImageProvider.getMethodImageDescriptor(false, modifiers);
		return decorateImageDescriptor(desc, modifiers);
	}
	
	/**
	 * Returns a version of <code>descriptor</code> decorated according to
	 * the passed <code>modifier</code> flags.
	 *  
	 * @param descriptor the image descriptor to decorate
	 * @param flags the modifier flags
	 * @return an image descriptor for a method proposal
	 * @see Flags
	 */
	protected final ImageDescriptor decorateImageDescriptor(ImageDescriptor descriptor, int flags) {
		int adornments= 0;
		
		if (Flags.isDeprecated(flags))
			adornments |= JavaElementImageDescriptor.DEPRECATED;
	 	
		if (Flags.isStatic(flags))
			adornments |= JavaElementImageDescriptor.STATIC;
	
		if (Flags.isFinal(flags))
			adornments |= JavaElementImageDescriptor.FINAL;
		
		if (Flags.isSynchronized(flags))
			adornments |= JavaElementImageDescriptor.SYNCHRONIZED;
		
		if (Flags.isAbstract(flags))
			adornments |= JavaElementImageDescriptor.ABSTRACT;
		
		return new JavaElementImageDescriptor(descriptor, adornments, JavaElementImageProvider.SMALL_SIZE);
	}
	
	/**
	 * Creates a display label for the given method proposal.
	 * 
	 * @param methodProposal the method proposal to display
	 * @param parameterList the already computed parameter list
	 * @return the display label for the given method proposal
	 */
	protected final String createMethodDisplayString(CompletionProposal methodProposal, String parameterList) {
		StringBuffer nameBuffer= new StringBuffer();
		nameBuffer.append(methodProposal.getName());
		nameBuffer.append('(');
		nameBuffer.append(parameterList);
		nameBuffer.append(")  "); //$NON-NLS-1$
		
		char[] returnType= createTypeDisplayName(SignatureUtil.getUpperBound(Signature.getReturnType(SignatureUtil.fix83600(methodProposal.getSignature()))));
		nameBuffer.append(returnType);

		String declaringType= extractTypeFQN(methodProposal);
		declaringType= Signature.getSimpleName(declaringType);
		nameBuffer.append(" - "); //$NON-NLS-1$
		nameBuffer.append(declaringType);

		return nameBuffer.toString();
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
		if (fCompilationUnit == null)
			return;
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
		String packageName= null;
		String className= null;
		for (int index= 0; index < parameterTypeNames.length; index++) {
			packageName= String.valueOf(parameterPackageNames[index]);
			className= String.valueOf(parameterTypeNames[index]);
			if (packageName.length() > 0)
				paramTypes[index]= packageName + "." + className; //$NON-NLS-1$
			else
				paramTypes[index]= className;
		}

		JavaCompletionProposal proposal= new OverrideCompletionProposal(fJavaProject, fCompilationUnit, String.valueOf(name), paramTypes, start, getLength(start, end), displayString.toString(), String.valueOf(completionName));
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
	
	protected final Image getImage(ImageDescriptor descriptor) {
		return (descriptor == null) ? null : fRegistry.get(descriptor);
	}
	
	protected void internalAcceptAnnotationAttributeReference(CompletionProposal proposal) {
		String displayString= createAnnotationAttributeDisplayString(proposal).toString();
		ImageDescriptor descriptor= createMethodImageDescriptor(proposal.getFlags());
		String completion= String.valueOf(proposal.getCompletion());
		fMethods.add(new JavaCompletionProposal(completion, proposal.getReplaceStart(), getLength(proposal.getReplaceStart(), proposal.getReplaceEnd()), getImage(descriptor), displayString, proposal.getRelevance(), fTextViewer));
	}
	
	private StringBuffer createAnnotationAttributeDisplayString(CompletionProposal attributeProposal) {
		StringBuffer nameBuffer= new StringBuffer();
		nameBuffer.append(attributeProposal.getName()).append("  "); //$NON-NLS-1$
		
		char[] attributeType= createTypeDisplayName(SignatureUtil.getUpperBound(SignatureUtil.fix83600(attributeProposal.getSignature())));
		nameBuffer.append(attributeType);

		String declaringType= extractTypeFQN(attributeProposal);
		declaringType= Signature.getSimpleName(declaringType);
		nameBuffer.append(" - "); //$NON-NLS-1$
		nameBuffer.append(declaringType);

		return nameBuffer;
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
		if (isIgnored(proposal.getKind()))
			return;
		
		try {
			switch (proposal.getKind()) {
				case CompletionProposal.KEYWORD:
					internalAcceptKeyword(proposal.getName(), proposal.getReplaceStart(), proposal.getReplaceEnd(), proposal.getRelevance());
					break;
				case CompletionProposal.PACKAGE_REF:
					internalAcceptPackage(proposal.getDeclarationSignature(), proposal.getCompletion(), proposal.getReplaceStart(), proposal.getReplaceEnd(), proposal.getRelevance());
					break;
				case CompletionProposal.TYPE_REF:
					internalAcceptType(proposal);
					break;
				case CompletionProposal.FIELD_REF:
					char[] signatureSimpleName= Signature.getSignatureSimpleName(proposal.getSignature());
					if (signatureSimpleName == null)
						signatureSimpleName= CharOperation.NO_CHAR;
					internalAcceptField(Signature.getSignatureQualifier(proposal.getDeclarationSignature()), Signature.getSignatureSimpleName(proposal.getDeclarationSignature()), proposal.getName(),
							Signature.getSignatureQualifier(proposal.getSignature()), signatureSimpleName, proposal.getCompletion(), proposal.getFlags(), proposal.getReplaceStart(), proposal
									.getReplaceEnd(), proposal.getRelevance());
					break;
				case CompletionProposal.METHOD_REF:
					acceptMethodProposal(proposal);
					break;
				case CompletionProposal.METHOD_NAME_REFERENCE:
					acceptMethodProposal(proposal);
					break;
				case CompletionProposal.METHOD_DECLARATION:
					internalAcceptMethodDeclaration(Signature.getSignatureQualifier(proposal.getDeclarationSignature()), Signature.getSignatureSimpleName(proposal.getDeclarationSignature()), proposal
							.getName(), getParameterPackages(proposal.getSignature()), getParameterTypes(proposal.getSignature()),
							proposal.findParameterNames(null) == null ? CharOperation.NO_CHAR_CHAR : proposal.findParameterNames(null), Signature.getSignatureQualifier(Signature
									.getReturnType(proposal.getSignature())), Signature.getSignatureSimpleName(Signature.getReturnType(proposal.getSignature())), proposal.getCompletion(), proposal
									.getFlags(), proposal.getReplaceStart(), proposal.getReplaceEnd(), proposal.getRelevance());
					break;
				case CompletionProposal.ANONYMOUS_CLASS_DECLARATION:
					internalAcceptAnonymousType(Signature.getSignatureQualifier(proposal.getDeclarationSignature()), Signature.getSignatureSimpleName(proposal.getDeclarationSignature()),
							getParameterPackages(proposal.getSignature()), getParameterTypes(proposal.getSignature()), proposal.findParameterNames(null) == null ? CharOperation.NO_CHAR_CHAR
									: proposal.findParameterNames(null), proposal.getCompletion(), proposal.getFlags(), proposal.getReplaceStart(), proposal.getReplaceEnd(), proposal.getRelevance());
					break;
				case CompletionProposal.LABEL_REF:
					internalAcceptLabel(proposal.getCompletion(), proposal.getReplaceStart(), proposal.getReplaceEnd(), proposal.getRelevance());
					break;
				case CompletionProposal.LOCAL_VARIABLE_REF:
					signatureSimpleName= Signature.getSignatureSimpleName(proposal.getSignature());
					if (signatureSimpleName == null)
						signatureSimpleName= CharOperation.NO_CHAR;
					internalAcceptLocalVariable(proposal.getCompletion(), Signature.getSignatureQualifier(proposal.getSignature()), signatureSimpleName, proposal.getFlags(), proposal
							.getReplaceStart(), proposal.getReplaceEnd(), proposal.getRelevance());
					break;
				case CompletionProposal.VARIABLE_DECLARATION:
					signatureSimpleName= Signature.getSignatureSimpleName(proposal.getSignature());
					if (signatureSimpleName == null)
						signatureSimpleName= CharOperation.NO_CHAR;
					internalAcceptLocalVariable(proposal.getCompletion(), Signature.getSignatureQualifier(proposal.getSignature()), signatureSimpleName, proposal.getFlags(), proposal
							.getReplaceStart(), proposal.getReplaceEnd(), proposal.getRelevance());
					break;
				case CompletionProposal.POTENTIAL_METHOD_DECLARATION:
					internalAcceptPotentialMethodDeclaration(Signature.getSignatureQualifier(proposal.getDeclarationSignature()), Signature.getSignatureSimpleName(proposal.getDeclarationSignature()),
							proposal.getName(), proposal.getReplaceStart(), proposal.getReplaceEnd(), proposal.getRelevance());
					break;
				case CompletionProposal.ANNOTATION_ATTRIBUTE_REF:
					internalAcceptAnnotationAttributeReference(proposal);
					break;
			}
		} catch (IllegalArgumentException e) {
			// all signature processing method may throw IAEs
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=84657
			// don't abort, but log and show all the valid proposals
			JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, "Exception when processing proposal for: " + String.valueOf(proposal.getName()), e)); //$NON-NLS-1$
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
}
