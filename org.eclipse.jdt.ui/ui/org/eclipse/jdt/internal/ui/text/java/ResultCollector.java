package org.eclipse.jdt.internal.ui.text.java;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
  
import java.util.ArrayList;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.core.resources.IMarker;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICodeCompletionRequestor;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.ui.JavaPluginImages;



/**
 * Bin to collect the proposal of the infrastructure on code assist in a java text.
 */
public class ResultCollector implements ICodeCompletionRequestor {
	
	protected ArrayList fFields= new ArrayList(), fKeywords= new ArrayList(), 
						fLabels= new ArrayList(), fMethods= new ArrayList(), 
						fModifiers= new ArrayList(), fPackages= new ArrayList(),
						fTypes= new ArrayList(), fVariables= new ArrayList();

	protected IMarker fLastProblem;
	
	protected IJavaProject fJavaProject;
	
	protected ArrayList[] fResults = new ArrayList[] {
		fVariables, fFields, fMethods, fTypes, fKeywords, fModifiers, fLabels, fPackages
	};
	
	protected int fOffset;
	protected int fLength;
	
	
	/**
	 * @see ICompletionRequestor#acceptClass
	 */	
	public void acceptClass(char[] packageName, char[] typeName, char[] completionName, int modifiers, int start, int end) {
		ProposalInfo info= new ProposalInfo(fJavaProject, packageName, typeName);
		fTypes.add(createCompletion(start, end, new String(completionName), JavaPluginImages.IMG_OBJS_CLASS, new String(typeName), new String(packageName), false, info));
	}
	
	/**
	 * @see ICompletionRequestor#acceptError
	 */	
	public void acceptError(IMarker problemMarker) {
		fLastProblem= problemMarker;
	}
	
	/**
	 * @see ICompletionRequestor#acceptField
	 */	
	public void acceptField(
		char[] declaringTypePackageName, char[] declaringTypeName, char[] name,
		char[] typePackageName, char[] typeName, char[] completionName,
		int modifiers, int start, int end) {
	
		String iconName= JavaPluginImages.IMG_MISC_DEFAULT;
		if (Flags.isPublic(modifiers)) {
			iconName= JavaPluginImages.IMG_MISC_PUBLIC;
		} else if (Flags.isProtected(modifiers)) {
			iconName= JavaPluginImages.IMG_MISC_PROTECTED;
		} else if (Flags.isPrivate(modifiers)) {
			iconName= JavaPluginImages.IMG_MISC_PRIVATE;
		}
	
		StringBuffer nameBuffer= new StringBuffer();
		nameBuffer.append(name);
		if (typeName.length > 0) {
			nameBuffer.append("   "); //$NON-NLS-1$
			nameBuffer.append(typeName);
		}
	
		String declaringClass= null;
		if (declaringTypeName.length > 0)
			declaringClass= new String(declaringTypeName);
			
		ProposalInfo info= new ProposalInfo(fJavaProject, declaringTypePackageName, declaringTypeName, name);
		fFields.add(createCompletion(start, end, new String(completionName), iconName, nameBuffer.toString(), declaringClass, false, info));
	}
	
	/**
	 * @see ICompletionRequestor#acceptInterface
	 */	
	public void acceptInterface(char[] packageName, char[] typeName, char[] completionName, int modifiers, int start, int end) {
		ProposalInfo info= new ProposalInfo(fJavaProject, packageName, typeName);
		fTypes.add(createCompletion(start, end, new String(completionName), JavaPluginImages.IMG_OBJS_INTERFACE, new String(typeName), new String(packageName), false, info));
	}
	
	/**
	 * @see ICompletionRequestor#acceptKeyword
	 */	
	public void acceptKeyword(char[] keyword, int start, int end) {
		String kw= new String(keyword);
		fKeywords.add(createCompletion(start, end, kw, null, kw, null, true, null));
	}
	
	/**
	 * @see ICompletionRequestor#acceptLabel
	 */	
	public void acceptLabel(char[] labelName, int start, int end) {
		String ln= new String(labelName);
		fLabels.add(createCompletion(start, end, ln, null, ln, null, false, null));
	}
	
	/**
	 * @see ICompletionRequestor#acceptLocalVariable
	 */	
	public void acceptLocalVariable(char[] name, char[] typePackageName, char[] typeName, int modifiers, int start, int end) {
		StringBuffer nameBuffer= new StringBuffer();
		nameBuffer.append(name);
		nameBuffer.append("   "); //$NON-NLS-1$
		nameBuffer.append(typeName);
	
		fVariables.add(createCompletion(start, end, new String(name), null, nameBuffer.toString(), null, false, null));
	}
	
	/**
	 * @see ICompletionRequestor#acceptMethod
	 */	
	public void acceptMethod(
		char[] declaringTypePackageName, char[] declaringTypeName, char[] name,
		char[][] parameterPackageNames, char[][] parameterTypeNames,
		char[] returnTypePackageName, char[] returnTypeName,
		char[] completionName,
		int modifiers, int start, int end) {
	
		String iconName= JavaPluginImages.IMG_MISC_DEFAULT;
		if (Flags.isPublic(modifiers)) {
			iconName= JavaPluginImages.IMG_MISC_PUBLIC;
		} else if (Flags.isProtected(modifiers)) {
			iconName= JavaPluginImages.IMG_MISC_PROTECTED;
		} else if (Flags.isPrivate(modifiers)) {
			iconName= JavaPluginImages.IMG_MISC_PRIVATE;
		}
	
		
		ProposalContextInformation contextInformation= null;
		StringBuffer nameBuffer= new StringBuffer();
		nameBuffer.append(name);
		nameBuffer.append('(');
		if (parameterTypeNames != null) {
			int length= parameterTypeNames.length;
			if (length > 0) {
				StringBuffer paramBuffer= new StringBuffer();
				for (int i= 0; i < length; i++) {
					if (i != 0) 
						paramBuffer.append(',');
					paramBuffer.append(parameterTypeNames[i]);
				}
				contextInformation= new ProposalContextInformation();
				String parameters= paramBuffer.toString();
				contextInformation.setInformationDisplayString(parameters);
				nameBuffer.append(parameters);
			}
		}
		nameBuffer.append(")   "); //$NON-NLS-1$
		nameBuffer.append(returnTypeName);
		
		String signature= nameBuffer.toString();
		if (contextInformation != null)
			contextInformation.setContextDisplayString(signature);
		
		ProposalInfo info= new ProposalInfo(fJavaProject, declaringTypePackageName, declaringTypeName, name, parameterPackageNames, parameterTypeNames);
	
		boolean hasClosingBracket= completionName.length > 0 && completionName[completionName.length - 1] == ')';
		if (!hasClosingBracket && completionName.length > 0) {
			// it's just a method name and no parameter list
			contextInformation= null;
		}
			
		boolean userMustCompleteParameters= (contextInformation != null && completionName.length > 0);
		fMethods.add(createCompletion(start, end, new String(completionName), iconName, signature, new String(declaringTypeName), false, !userMustCompleteParameters, contextInformation, info));
	}
	
	/**
	 * @see ICompletionRequestor#acceptModifier
	 */	
	public void acceptModifier(char[] modifier, int start, int end) {
		String mod= new String(modifier);
		fModifiers.add(createCompletion(start, end, mod, null, mod, null, true, null));
	}
	
	/**
	 * @see ICompletionRequestor#acceptPackage
	 */	
	public void acceptPackage(char[] packageName, char[] completionName, int start, int end) {
		fPackages.add(createCompletion(start, end, new String(completionName), JavaPluginImages.IMG_OBJS_PACKAGE, new String(packageName), null, false, null));
	}
	
	/**
	 * @see ICompletionRequestor#acceptType
	 */	
	public void acceptType(char[] packageName, char[] typeName, char[] completionName, int start, int end) {
		ProposalInfo info= new ProposalInfo(fJavaProject, packageName, typeName);
		fTypes.add(createCompletion(start, end, new String(completionName), JavaPluginImages.IMG_OBJS_CLASS, new String(typeName), new String(packageName), false, info));
	}
	
	public String getErrorMessage() {
		if (fLastProblem != null)
			return fLastProblem.getAttribute(IMarker.MESSAGE, JavaTextMessages.getString("ResultCollector.compile_error.message")); //$NON-NLS-1$
		return ""; //$NON-NLS-1$
	}

	public ICompletionProposal[] getResults() {
		ArrayList result= new ArrayList();
		for (int i= 0; i < fResults.length; i++) {
			ArrayList bucket = fResults[i];
			int size= bucket.size();
			if (size == 1) {
				result.add(bucket.get(0));
			} else if (size > 1) {
				Object[] sortedBucket = new Object[size];
				bucket.toArray(sortedBucket);
				quickSort(sortedBucket, 0, size - 1);
				for (int j= 0; j < sortedBucket.length; j++)
					result.add(sortedBucket[j]);
			}
		}		
		return (ICompletionProposal[]) result.toArray(new ICompletionProposal[result.size()]);
	}
	
	protected Image getIcon(String name) {
		if (name != null)
			return JavaPluginImages.get(name);
		return null;
	}
	
	protected Object createCompletion(int start, int end, String completion, String iconName, String name, String qualification, boolean isKeyWord, ProposalInfo proposalInfo) {
		return createCompletion(start, end, completion, iconName, name, qualification, isKeyWord, true, null, proposalInfo);
	}
	
	protected Object createCompletion(int start, int end, String completion, String iconName, String name, String qualification, boolean isKeyWord, boolean placeCursorBehindInsertion, ProposalContextInformation contextInformation, ProposalInfo proposalInfo) {
		
		if (qualification != null)
			name += (" - " + qualification); //$NON-NLS-1$
			
		int cursorPosition= completion == null ? 0 : completion.length();
		if (!placeCursorBehindInsertion)
			-- cursorPosition;
		
		int length= end - start;
		if (fOffset > -1 && fLength > -1)
			length= fLength + (fOffset - start);
		
		Image icon= getIcon(iconName);
		if (contextInformation != null)
			contextInformation.setImage(icon);
			
		return new JavaCompletionProposal(completion, start, length, cursorPosition, icon, name, contextInformation, proposalInfo);
	} 
		
	protected int compare(Object o1, Object o2) {
		ICompletionProposal c1= (ICompletionProposal) o1;
		ICompletionProposal c2= (ICompletionProposal) o2;
		return c2.getDisplayString().compareTo(c1.getDisplayString());
	}
	
	protected Object[] quickSort(Object[] collection, int left, int right) {
		int original_left= left;
		int original_right= right;
		Object mid= collection[(left + right) / 2];
		
		do {
			
			while (compare(collection[left], mid) > 0) // s[left] >= mid
				left++;
			
			while (compare(collection[right], mid) < 0) // s[right] <= mid
				right--;
			
			if (left <= right) {
				Object tmp= collection[left];
				collection[left]= collection[right];
				collection[right]= tmp;
				left++;
				right--;
			}
		} while (left <= right);
		
		if (original_left < right)
			collection= quickSort(collection, original_left, right);
		
		if (left < original_right)
			collection= quickSort(collection, left, original_right);
		
		return collection;
	}
	
	public void reset(IJavaProject jproject) {
		fJavaProject= jproject;
		
		fOffset= -1;
		fLength= -1;
		
		fLastProblem= null;
		
		for (int i= 0; i < fResults.length; i++)
			fResults[i].clear();
	}
	
	/**
	 * If the region is set, it overrules the range specified by
	 * the content assist infrastructure.
	 */
	public void setRegionToReplace(int offset, int length) {
		fOffset= offset;
		fLength= length;
	}
}
