/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.tagging.IRenameRefactoring;import org.eclipse.jdt.internal.core.refactoring.DebugUtils;import org.eclipse.jdt.internal.ui.util.JdtHackFinder;

public class RenameRefactoringWizard extends RefactoringWizard {
	
	private String fPageMessage;
	
	private String fPageContextHelpId;
	
	private static final String INPUTPAGE_TITLE_SUFFIX= ".wizard.inputpage.title";
	private static final String INPUTPAGE_MESSAGE_SUFFIX= ".wizard.inputpage.message";
	
	public RenameRefactoringWizard(String resourceKeyPrefix, String pageContextHelpId, String errorContextHelpId){
		super(getInputPageResource(resourceKeyPrefix, INPUTPAGE_TITLE_SUFFIX), errorContextHelpId);
		fPageMessage= getInputPageResource(resourceKeyPrefix, INPUTPAGE_MESSAGE_SUFFIX);
		fPageContextHelpId= pageContextHelpId;
	}
	
	private static String getInputPageResource(String prefix, String suffix){
		return RefactoringResources.getResourceString(prefix + suffix);
	}

	/**
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		String initialSetting= getRenameRefactoring().getCurrentName();
		setPageTitle(getPageTitle() + ": "+ initialSetting);
		RenameInputWizardPage page= new RenameInputWizardPage(fPageContextHelpId, true, initialSetting) {
			protected RefactoringStatus validateTextField(String text) {
				return validateNewName(text);
			}	
		};
		page.setMessage(fPageMessage);
		addPage(page);
	}
	
	private IRenameRefactoring getRenameRefactoring(){
		return (IRenameRefactoring)getRefactoring();	
	}
	
	private RefactoringStatus validateNewName(String newName){
		IRenameRefactoring ref= getRenameRefactoring();
		ref.setNewName(newName);
		try{
			return ref.checkNewName();
		} catch (JavaModelException e){
			JdtHackFinder.fixMeSoon("should log the exception");
			String msg= e.getMessage() == null ? "": e.getMessage();
			RefactoringStatus result= new RefactoringStatus();
			result.addFatalError("JavaModelException during name checking:" + msg);
			return result;
		}	
	}
	
	protected boolean checkActivationOnOpen() {
		return true;
	}
}