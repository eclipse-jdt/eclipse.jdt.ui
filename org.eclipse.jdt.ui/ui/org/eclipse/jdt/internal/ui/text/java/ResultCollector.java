package org.eclipse.jdt.internal.ui.text.java;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
  
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.resources.IMarker;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ICompletionRequestor;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;

/**
 * Bin to collect the proposal of the infrastructure on code assist in a java text.
 */
public class ResultCollector implements ICompletionRequestor {
		
	private class ProposalComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			ICompletionProposal c1= (ICompletionProposal) o1;
			ICompletionProposal c2= (ICompletionProposal) o2;
			return c1.getDisplayString().compareToIgnoreCase(c2.getDisplayString());
		}
	};

	private final static char[] METHOD_WITH_ARGUMENTS_TRIGGERS= new char[] { '(', '-', ' ' };
	private final static char[] METHOD_TRIGGERS= new char[] { ';', ',', '.', '\t', '[', ' ' };
	private final static char[] TYPE_TRIGGERS= new char[] { '.', '\t', '[', '(', ' ' };
	private final static char[] VAR_TRIGGER= new char[] { '\t', ' ', '=', ';' };
	
	private ArrayList fFields= new ArrayList(), fKeywords= new ArrayList(), 
						fLabels= new ArrayList(), fMethods= new ArrayList(),
						fModifiers= new ArrayList(), fPackages= new ArrayList(),
						fTypes= new ArrayList(), fVariables= new ArrayList();

	private IProblem fLastProblem;
	
	private IJavaProject fJavaProject;
	private ICompilationUnit fCompilationUnit; // set when imports can be added
	private int fCodeAssistOffset;
	private ImageDescriptorRegistry fRegistry= JavaPlugin.getImageDescriptorRegistry();
	
	private ArrayList[] fResults = new ArrayList[] {
		fVariables, fFields, fMethods, fTypes, fKeywords, fModifiers, fLabels, fPackages
	};
	
	private int fUserReplacementLength;

	/*
	 * Is eating code assist enabled or disabled? PR #3666
	 * When eating is enabled, JavaCompletionProposal must be revisited: PR #5533
	 */
	private boolean fPreventEating= true;	
	
	/*
	 * @see ICompletionRequestor#acceptClass
	 */	
	public void acceptClass(char[] packageName, char[] typeName, char[] completionName, int modifiers, int start, int end) {
		ImageDescriptor descriptor= JavaPluginImages.DESC_OBJS_CLASS;
		if (Flags.isDeprecated(modifiers))
			descriptor= getDeprecatedDescriptor(descriptor);

		ProposalInfo info= new ProposalInfo(fJavaProject, packageName, typeName);
		fTypes.add(createTypeCompletion(start, end, new String(completionName), descriptor, new String(typeName), new String(packageName), info));
	}
	
	/*
	 * @see ICompletionRequestor#acceptError(IProblem)
	 */
	public void acceptError(IProblem error) {
		fLastProblem= error;
	}	
	
	/*
	 * @see ICompletionRequestor#acceptField
	 */	
	public void acceptField(
		char[] declaringTypePackageName, char[] declaringTypeName, char[] name,
		char[] typePackageName, char[] typeName, char[] completionName,
		int modifiers, int start, int end) {

		ImageDescriptor descriptor= getMemberDescriptor(modifiers);	
		if (Flags.isDeprecated(modifiers))
		 	descriptor= getDeprecatedDescriptor(descriptor);	
		
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
		
		JavaCompletionProposal proposal= createCompletion(start, end, new String(completionName), descriptor, nameBuffer.toString());
		proposal.setProposalInfo(new ProposalInfo(fJavaProject, declaringTypePackageName, declaringTypeName, name));
		proposal.setTriggerCharacters(VAR_TRIGGER);
		
		fFields.add(proposal);
	}
	
	/*
	 * @see ICompletionRequestor#acceptInterface
	 */	
	public void acceptInterface(char[] packageName, char[] typeName, char[] completionName, int modifiers, int start, int end) {
		ImageDescriptor descriptor= JavaPluginImages.DESC_OBJS_INTERFACE;
		if (Flags.isDeprecated(modifiers))
			descriptor= getDeprecatedDescriptor(descriptor);

		ProposalInfo info= new ProposalInfo(fJavaProject, packageName, typeName);
		fTypes.add(createTypeCompletion(start, end, new String(completionName), descriptor, new String(typeName), new String(packageName), info));
	}
	
	/*
	 * @see ICompletionRequestor#acceptAnonymousType(char[], char[], char[][], char[][], char[][], char[], int, int, int)
	 */
	public void acceptAnonymousType(char[] superTypePackageName, char[] superTypeName, char[][] parameterPackageNames, char[][] parameterTypeNames, char[][] parameterNames,
		char[] completionName, int modifiers, int completionStart, int completionEnd) {

		JavaCompletionProposal	proposal= createAnonymousTypeCompletion(superTypePackageName, superTypeName, parameterTypeNames, parameterNames, completionName, completionStart, completionEnd);
		proposal.setProposalInfo(new ProposalInfo(fJavaProject, superTypePackageName, superTypeName));
		fTypes.add(proposal);
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
			buf.append("    "); //$NON-NLS-1$
			buf.append(typeName);
		}
		JavaCompletionProposal proposal= createCompletion(start, end, new String(name), null, buf.toString());
		proposal.setTriggerCharacters(VAR_TRIGGER);
		fVariables.add(proposal);
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
	 * @see ICompletionRequestor#acceptMethod(char[], char[], char[], char[][], char[][], char[][], char[], char[], char[], int, int, int)
	 */
	public void acceptMethod(char[] declaringTypePackageName, char[] declaringTypeName, char[] name,
		char[][] parameterPackageNames, char[][] parameterTypeNames, char[][] parameterNames,
		char[] returnTypePackageName, char[] returnTypeName, char[] completionName, int modifiers,
		int start, int end) {
	
		JavaCompletionProposal proposal= createMethodCompletion(declaringTypeName, name, parameterTypeNames, parameterNames, returnTypeName, completionName, modifiers, start, end);
		proposal.setProposalInfo(new ProposalInfo(fJavaProject, declaringTypePackageName, declaringTypeName, name, parameterPackageNames, parameterTypeNames, returnTypeName.length == 0));

		boolean hasOpeningBracket= completionName.length == 0 || (completionName.length > 0 && completionName[completionName.length - 1] == ')');
	
		ProposalContextInformation contextInformation= null;
		if (hasOpeningBracket && parameterTypeNames.length > 0) {
			contextInformation= new ProposalContextInformation();
			contextInformation.setInformationDisplayString(getParameterSignature(parameterTypeNames, parameterNames));		
			contextInformation.setContextDisplayString(proposal.getDisplayString());
			contextInformation.setImage(proposal.getImage());		
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
	public void acceptModifier(char[] modifier, int start, int end) {
		String mod= new String(modifier);
		fModifiers.add(createCompletion(start, end, mod, null, mod));
	}
	
	/*
	 * @see ICompletionRequestor#acceptPackage
	 */	
	public void acceptPackage(char[] packageName, char[] completionName, int start, int end) {
		fPackages.add(createCompletion(start, end, new String(completionName), JavaPluginImages.DESC_OBJS_PACKAGE, new String(packageName)));
	}
	
	/*
	 * @see ICompletionRequestor#acceptType
	 */	
	public void acceptType(char[] packageName, char[] typeName, char[] completionName, int start, int end) {
		ProposalInfo info= new ProposalInfo(fJavaProject, packageName, typeName);
		fTypes.add(createTypeCompletion(start, end, new String(completionName), JavaPluginImages.DESC_OBJS_CLASS, new String(typeName), new String(packageName), info));
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
			buf.append(" - "); //$NON-NLS-1$
			buf.append(typeName);
		}
		JavaCompletionProposal proposal= createCompletion(start, end, new String(completionName), null, buf.toString());
		proposal.setTriggerCharacters(VAR_TRIGGER);
		fVariables.add(proposal);
	}	
	
	public String getErrorMessage() {
		if (fLastProblem != null)
			return fLastProblem.getMessage();
		return ""; //$NON-NLS-1$
	}

	public JavaCompletionProposal[] getResults() {
		ArrayList result= new ArrayList();
		ProposalComparator comperator= new ProposalComparator();
		for (int i= 0; i < fResults.length; i++) {
			ArrayList bucket = fResults[i];
			int size= bucket.size();
			if (size == 1) {
				result.add(bucket.get(0));
			} else if (size > 1) {
				Object[] sortedBucket = new Object[size];
				bucket.toArray(sortedBucket);
				Arrays.sort(sortedBucket, comperator);
				for (int j= 0; j < sortedBucket.length; j++)
					result.add(sortedBucket[j]);
			}
		}		
		return (JavaCompletionProposal[]) result.toArray(new JavaCompletionProposal[result.size()]);
	}

	protected JavaCompletionProposal createMethodCompletion(char[] declaringTypeName, char[] name, char[][] parameterTypeNames, char[][] parameterNames, char[] returnTypeName, char[] completionName, int modifiers, int start, int end) {
		ImageDescriptor descriptor= getMemberDescriptor(modifiers);
		if (Flags.isDeprecated(modifiers))
		 	descriptor= getDeprecatedDescriptor(descriptor);

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
		return createCompletion(start, end, new String(completionName), descriptor, nameBuffer.toString());
	}

	protected JavaCompletionProposal createAnonymousTypeCompletion(char[] declaringTypePackageName, char[] declaringTypeName, char[][] parameterTypeNames, char[][] parameterNames, char[] completionName, int start, int end) {
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
		nameBuffer.append("  ");
		nameBuffer.append(JavaTextMessages.getString("ResultCollector.anonymous_type")); //$NON-NLS-1$
	
		int length= end - start;
		
		return new AnonymousTypeCompletionProposal(fJavaProject, fCompilationUnit, start, length, new String(completionName), nameBuffer.toString(), declTypeBuf.toString());
	}

	
	protected JavaCompletionProposal createTypeCompletion(int start, int end, String completion, ImageDescriptor descriptor, String typeName, String containerName, ProposalInfo proposalInfo) {
		IImportDeclaration importDeclaration= null;
		if (containerName != null && fCompilationUnit != null) {
			if (completion.equals(JavaModelUtil.concatenateName(containerName, typeName))) {
				importDeclaration= fCompilationUnit.getImport(completion);
				completion= typeName;
			}
		}
		StringBuffer buf= new StringBuffer(typeName);
		if (containerName != null) {
			buf.append(" - "); //$NON-NLS-1$
			if (containerName.length() > 0) {
				buf.append(containerName);
			} else {
				buf.append(JavaTextMessages.getString("ResultCollector.default_package"));
			}
		}
		String name= buf.toString();
		
		JavaCompletionProposal proposal= createCompletion(start, end, completion, descriptor, name);
		proposal.setImportDeclaration(importDeclaration);
		proposal.setProposalInfo(proposalInfo);
		proposal.setTriggerCharacters(TYPE_TRIGGERS);
		return proposal;
	}

	protected ImageDescriptor getMemberDescriptor(int modifiers) {
		if (Flags.isPublic(modifiers)) {
			return JavaPluginImages.DESC_MISC_PUBLIC;
		} else if (Flags.isProtected(modifiers)) {
			return JavaPluginImages.DESC_MISC_PROTECTED;
		} else if (Flags.isPrivate(modifiers)) {
			return JavaPluginImages.DESC_MISC_PRIVATE;
		} else {
			return JavaPluginImages.DESC_MISC_DEFAULT;
		}
	}
	
	protected ImageDescriptor getDeprecatedDescriptor(ImageDescriptor descriptor) {
		Point size= new Point(16, 16);
		return new JavaElementImageDescriptor(descriptor, JavaElementImageDescriptor.WARNING, size);	    
	}
	
	protected JavaCompletionProposal createCompletion(int start, int end, String completion, ImageDescriptor descriptor, String name) {
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

		Image icon= (descriptor == null) ? null : fRegistry.get(descriptor);
		return new JavaCompletionProposal(completion, start, length, icon, name);
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
		fJavaProject= jproject;
		fCompilationUnit= cu;
		fCodeAssistOffset= codeAssistOffset;
		
		fUserReplacementLength= -1;
		
		fLastProblem= null;
		
		for (int i= 0; i < fResults.length; i++)
			fResults[i].clear();
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

}
