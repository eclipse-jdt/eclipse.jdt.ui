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
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.JavaModelUtil;

/**
 * Bin to collect the proposal of the infrastructure on code assist in a java text.
 */
public class ResultCollector implements ICodeCompletionRequestor {
	
	
	private final static char[] METHOD_WITH_ARGUMENTS_TRIGGERS= new char[] { '(', '-' };
	private final static char[] GENERAL_TRIGGERS= new char[] { ';', ',', '.', '\t', '(', '{', '[' };
	
	private ArrayList fFields= new ArrayList(), fKeywords= new ArrayList(), 
						fLabels= new ArrayList(), fMethods= new ArrayList(), 
						fModifiers= new ArrayList(), fPackages= new ArrayList(),
						fTypes= new ArrayList(), fVariables= new ArrayList();

	private IMarker fLastProblem;
	
	private IJavaProject fJavaProject;
	private ICompilationUnit fCompilationUnit;
	
	private ArrayList[] fResults = new ArrayList[] {
		fVariables, fFields, fMethods, fTypes, fKeywords, fModifiers, fLabels, fPackages
	};
	
	private int fUserReplacementLength;
	private int fUserReplacementOffset;
	
	
	/*
	 * @see ICompletionRequestor#acceptClass
	 */	
	public void acceptClass(char[] packageName, char[] typeName, char[] completionName, int modifiers, int start, int end) {
		ProposalInfo info= new ProposalInfo(fJavaProject, packageName, typeName);
		fTypes.add(createTypeCompletion(start, end, new String(completionName), JavaPluginImages.IMG_OBJS_CLASS, new String(typeName), new String(packageName), info));
	}
	
	/*
	 * @see ICompletionRequestor#acceptError
	 */	
	public void acceptError(IMarker problemMarker) {
		fLastProblem= problemMarker;
	}
	
	/*
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
		if (declaringTypeName != null && declaringTypeName.length > 0) {
			nameBuffer.append(" - "); //$NON-NLS-1$
			nameBuffer.append(declaringTypeName);
		}	
		
		JavaCompletionProposal proposal= createCompletion(start, end, new String(completionName), iconName, nameBuffer.toString());
		proposal.setProposalInfo(new ProposalInfo(fJavaProject, declaringTypePackageName, declaringTypeName, name));
		
		fFields.add(proposal);
	}
	
	/*
	 * @see ICompletionRequestor#acceptInterface
	 */	
	public void acceptInterface(char[] packageName, char[] typeName, char[] completionName, int modifiers, int start, int end) {
		ProposalInfo info= new ProposalInfo(fJavaProject, packageName, typeName);
		fTypes.add(createTypeCompletion(start, end, new String(completionName), JavaPluginImages.IMG_OBJS_INTERFACE, new String(typeName), new String(packageName), info));
	}
	
	/*
	 * @see ICompletionRequestor#acceptKeyword
	 */	
	public void acceptKeyword(char[] keyword, int start, int end) {
		String kw= new String(keyword);
		fKeywords.add(createCompletion(start, end, kw, null, kw));
	}
	
	/*
	 * @see ICompletionRequestor#acceptLabel
	 */	
	public void acceptLabel(char[] labelName, int start, int end) {
		String ln= new String(labelName);
		fLabels.add(createCompletion(start, end, ln, null, ln));
	}
	
	/*
	 * @see ICompletionRequestor#acceptLocalVariable
	 */	
	public void acceptLocalVariable(char[] name, char[] typePackageName, char[] typeName, int modifiers, int start, int end) {
		StringBuffer buf= new StringBuffer();
		buf.append(name);
		if (typeName != null) {
			buf.append("    ");
			buf.append(typeName);
		}	
		fVariables.add(createCompletion(start, end, new String(name), null, buf.toString()));
	}
	
	private String getParameterSignature(char[][] parameterTypeNames, char[][] parameterNames) {
		StringBuffer buf = new StringBuffer();
		if (parameterTypeNames != null) {
			for (int i = 0; i < parameterTypeNames.length; i++) {
				if (i > 0) {
					buf.append(',');
					buf.append(' ');
				}
				buf.append(parameterTypeNames[i]);
				if (parameterNames[i] != null) {
					buf.append(' ');
					buf.append(parameterNames[i]);
				}
			}
		}
		return buf.toString();
	}
	
	/*
	 * @see ICodeCompletionRequestor#acceptMethod(char[], char[], char[], char[][], char[][], char[][], char[], char[], char[], int, int, int)
	 */
	public void acceptMethod(char[] declaringTypePackageName, char[] declaringTypeName, char[] name,
		char[][] parameterPackageNames, char[][] parameterTypeNames, char[][] parameterNames,
		char[] returnTypePackageName, char[] returnTypeName, char[] completionName, int modifiers,
		int start, int end) {
	
		JavaCompletionProposal proposal= createMethodCompletion(declaringTypeName, name, parameterTypeNames, parameterNames, returnTypeName, completionName, modifiers, start, end);
		proposal.setProposalInfo(new ProposalInfo(fJavaProject, declaringTypePackageName, declaringTypeName, name, parameterPackageNames, parameterTypeNames));

		boolean hasClosingBracket= completionName.length > 0 && completionName[completionName.length - 1] == ')';
	
		ProposalContextInformation contextInformation= null;
		if (hasClosingBracket && parameterTypeNames.length > 0) {
			contextInformation= new ProposalContextInformation();
			contextInformation.setInformationDisplayString(getParameterSignature(parameterTypeNames, parameterNames));		
			contextInformation.setContextDisplayString(proposal.getDisplayString());			
			proposal.setContextInformation(contextInformation);
		}
	
		boolean userMustCompleteParameters= (contextInformation != null && completionName.length > 0);
		char[] triggers= userMustCompleteParameters ? METHOD_WITH_ARGUMENTS_TRIGGERS : GENERAL_TRIGGERS;
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
	public void acceptModifier(char[] modifier, int start, int end) {
		String mod= new String(modifier);
		fModifiers.add(createCompletion(start, end, mod, null, mod));
	}
	
	/*
	 * @see ICompletionRequestor#acceptPackage
	 */	
	public void acceptPackage(char[] packageName, char[] completionName, int start, int end) {
		fPackages.add(createCompletion(start, end, new String(completionName), JavaPluginImages.IMG_OBJS_PACKAGE, new String(packageName)));
	}
	
	/*
	 * @see ICompletionRequestor#acceptType
	 */	
	public void acceptType(char[] packageName, char[] typeName, char[] completionName, int start, int end) {
		ProposalInfo info= new ProposalInfo(fJavaProject, packageName, typeName);
		fTypes.add(createTypeCompletion(start, end, new String(completionName), JavaPluginImages.IMG_OBJS_CLASS, new String(typeName), new String(packageName), info));
	}
	
	/*
	 * @see ICodeCompletionRequestor#acceptMethodDeclaration(char[], char[], char[], char[][], char[][], char[][], char[], char[], char[], int, int, int)
	 */
	public void acceptMethodDeclaration(char[] declaringTypePackageName, char[] declaringTypeName, char[] name, char[][] parameterPackageNames, char[][] parameterTypeNames, char[][] parameterNames, char[] returnTypePackageName, char[] returnTypeName, char[] completionName, int modifiers, int start, int end) {
		// XXX: To be revised
		JavaCompletionProposal proposal= createMethodCompletion(declaringTypeName, name, parameterTypeNames, parameterNames, returnTypeName, completionName, modifiers, start, end);
		fMethods.add(proposal);
	}
	
	/*
	 * @see ICodeCompletionRequestor#acceptVariableName(char[], char[], char[], char[], int, int)
	 */
	public void acceptVariableName(char[] typePackageName, char[] typeName, char[] name, char[] completionName, int start, int end) {
		// XXX: To be revised
		StringBuffer buf= new StringBuffer();
		buf.append(name);
		if (typeName != null && typeName.length > 0) {
			buf.append(" - ");
			buf.append(typeName);
		}	
		fVariables.add(createCompletion(start, end, new String(completionName), null, buf.toString()));
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

	protected JavaCompletionProposal createMethodCompletion(char[] declaringTypeName, char[] name, char[][] parameterTypeNames, char[][] parameterNames, char[] returnTypeName, char[] completionName, int modifiers, int start, int end) {
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
		nameBuffer.append('(');
		if (parameterTypeNames.length > 0) {
			nameBuffer.append(getParameterSignature(parameterTypeNames, parameterNames));
		}
		nameBuffer.append(')'); 
		if (returnTypeName.length > 0) {
			nameBuffer.append("  "); //$NON-NLS-1$
			nameBuffer.append(returnTypeName);
		}
		if (declaringTypeName.length > 0) {
			nameBuffer.append(" - "); //$NON-NLS-1$
			nameBuffer.append(declaringTypeName);
		}
		return createCompletion(start, end, new String(completionName), iconName, nameBuffer.toString());
	}

	
	protected JavaCompletionProposal createTypeCompletion(int start, int end, String completion, String iconName, String typeName, String containerName, ProposalInfo proposalInfo) {
		IImportDeclaration importDeclaration= null;
		if (containerName != null && fCompilationUnit != null) {
			if (completion.equals(JavaModelUtil.concatenateName(containerName, typeName))) {
				importDeclaration= fCompilationUnit.getImport(completion);
				completion= typeName;
			}
		}
		StringBuffer buf= new StringBuffer(typeName);
		if (containerName != null) {
			buf.append(" - ");
			buf.append(containerName);
		}
		String name= buf.toString();
		
		JavaCompletionProposal proposal= createCompletion(start, end, completion, iconName, name);
		proposal.setImportDeclaration(importDeclaration);
		proposal.setProposalInfo(proposalInfo);
		return proposal;
	}
	
	protected JavaCompletionProposal createCompletion(int start, int end, String completion, String iconName, String name) {
		int length;
		if (fUserReplacementLength == -1) {
			length= end - start;
		} else {
			length= fUserReplacementLength;
		}
		if (fUserReplacementOffset != -1) {
			start= fUserReplacementOffset;
		}
		
		Image icon= null;
		if (iconName != null)
			icon= JavaPluginImages.get(iconName);		

		return new JavaCompletionProposal(completion, start, length, icon, name);
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
	
	/**
	 * Specifies the context of the code assist operation.
	 * @param jproject The Java project to which the underlying source belongs.
	 * Needed to find types referred.
	 * @param cu The compilation unit that is edited. Used to add import statements.
	 * Can be <code>null</code> if no import statements should be added.
	 */
	public void reset(IJavaProject jproject, ICompilationUnit cu) {
		fJavaProject= jproject;
		fCompilationUnit= cu;
		
		fUserReplacementLength= -1;
		fUserReplacementOffset= -1;
		
		fLastProblem= null;
		
		for (int i= 0; i < fResults.length; i++)
			fResults[i].clear();
	}
	
	/**
	 * If the replacement length is set, this overrides the length returned from
	 * the content assist infrastructure.
	 * Use this setting if code assist is called with a none empty selection.
	 */
	public void setReplacementLength(int length) {
		fUserReplacementLength= length;
	}

	/**
	 * If the replacement offset is set this overrides the offset used for the content assist.
	 * Use this setting if the code assist proposals generated will be applied on a document different than
	 * the one used for evaluating the code assist.
	 */
	public void setReplacementOffset(int offset) {
		fUserReplacementOffset= offset;
	}

}
