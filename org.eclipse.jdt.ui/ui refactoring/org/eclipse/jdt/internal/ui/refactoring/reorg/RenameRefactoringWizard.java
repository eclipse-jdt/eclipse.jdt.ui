/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.refactoring.reorg;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenamingNameSuggestor;
import org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

public class RenameRefactoringWizard extends RefactoringWizard {

	private final String fInputPageDescription;

	private final String fPageContextHelpId;

	private final ImageDescriptor fInputPageImageDescriptor;

	static List<FieldDeclaration> fieldDeclarations= new ArrayList<>();

	static boolean flag= false;

	static String fieldDeclaration;
	// dialog settings constants:

	/**
	 * Dialog settings key (value is of type boolean).
	 */
	public static final String UPDATE_TEXTUAL_MATCHES= "updateTextualMatches"; //$NON-NLS-1$

	/**
	 * Dialog settings key (value is of type boolean).
	 */
	public static final String UPDATE_QUALIFIED_NAMES= "updateQualifiedNames"; //$NON-NLS-1$

	/**
	 * Dialog settings key (value is of type String).
	 */
	public static final String QUALIFIED_NAMES_PATTERNS= "patterns"; //$NON-NLS-1$

	/**
	 * Dialog settings key (value is of type boolean).
	 */
	public static final String TYPE_UPDATE_SIMILAR_ELEMENTS= "updateSimilarElements"; //$NON-NLS-1$

	/**
	 * Dialog settings key (value is of type int).
	 *
	 * @see RenamingNameSuggestor
	 */
	public static final String TYPE_SIMILAR_MATCH_STRATEGY= "updateSimilarElementsMatchStrategy"; //$NON-NLS-1$

	/**
	 * Dialog settings key (value is of type boolean).
	 */
	public static final String PACKAGE_RENAME_SUBPACKAGES= "renameSubpackages"; //$NON-NLS-1$

	/**
	 * Dialog settings key (value is of type boolean).
	 */
	public static final String FIELD_RENAME_GETTER= "renameGetter"; //$NON-NLS-1$

	/**
	 * Dialog settings key (value is of type boolean).
	 */
	public static final String FIELD_RENAME_SETTER= "renameSetter"; //$NON-NLS-1$

	public RenameRefactoringWizard(Refactoring refactoring, String defaultPageTitle, String inputPageDescription,
			ImageDescriptor inputPageImageDescriptor, String pageContextHelpId) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE);
		setDefaultPageTitle(defaultPageTitle);
		fInputPageDescription= inputPageDescription;
		fInputPageImageDescriptor= inputPageImageDescriptor;
		fPageContextHelpId= pageContextHelpId;
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
	}

	/*
	 * non java-doc
	 *
	 * @see RefactoringWizard#addUserInputPages
	 */
	@Override
	protected void addUserInputPages() {
		String initialSetting= getNameUpdating().getCurrentElementName();
		suggestedFieldName(initialSetting);
		RenameInputWizardPage inputPage= createInputPage(fInputPageDescription, initialSetting);
		inputPage.setImageDescriptor(fInputPageImageDescriptor);
		addPage(inputPage);
	}

	private INameUpdating getNameUpdating() {
		return getRefactoring().getAdapter(INameUpdating.class);
	}

	protected RenameInputWizardPage createInputPage(String message, String initialSetting) {
		return new RenameInputWizardPage(message, fPageContextHelpId, true, initialSetting) {
			@Override
			protected RefactoringStatus validateTextField(String text) {
				return validateNewName(text);
			}
		};
	}

	/**
	 * Sets a new name, validates the input, and returns the status.
	 *
	 * @param newName the new name
	 * @return validation status
	 */
	protected RefactoringStatus validateNewName(String newName) {
		INameUpdating ref= getNameUpdating();
		ref.setNewElementName(newName);
		try {
			return ref.checkNewElementName(newName);
		} catch (CoreException e) {
			JavaPlugin.log(e);
			return RefactoringStatus.createFatalErrorStatus(RefactoringMessages.RenameRefactoringWizard_internal_error);
		}
	}

	public static String suggestedFieldName(String initialSetting) {
		List<String> tempFieldList= new ArrayList<>();
		IEditorPart activeEditor= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		IEditorInput editorInput= activeEditor.getEditorInput();
		IFile file= editorInput.getAdapter(IFile.class);
		String filePath= file.getLocation().toOSString();
		CompilationUnit cu= getCompilationUnit(filePath);
		if (!fieldDeclarations.isEmpty()) {
			fieldDeclarations.clear();
		}
		getFieldDeclaration(cu, fieldDeclarations);
		String[] excluded= new String[fieldDeclarations.size()];
		if (fieldDeclarations.size() > 1) {
			for (int i= 0; i < fieldDeclarations.size(); i++) {
				FieldDeclaration fd= fieldDeclarations.get(i);
				VariableDeclarationFragment vdf= (VariableDeclarationFragment) fd.fragments().get(0);
				String fieldName= vdf.getName().getIdentifier();
				if (initialSetting.equals(fieldName)) {
					if (fd.toString().contains("static") && fd.toString().contains("final")) { //$NON-NLS-1$ //$NON-NLS-2$
						return initialSetting;
					}
				} else {
					fieldDeclaration= fd.toString();
					if ((!fieldDeclaration.contains("static") || !fieldDeclaration.contains("final")) && fieldName.length() > 1) { //$NON-NLS-1$ //$NON-NLS-2$
						excluded[i]= fieldName;
						tempFieldList.add(fieldName);
					}
				}
			}
		}
		return getProjectSpecificCovention(tempFieldList, initialSetting);
	}

	public static String getProjectSpecificCovention(List<String> fieldList, String initialSetting) {
		boolean notFlag= true;
		String pattern= "(?=[A-Z])|_"; //$NON-NLS-1$
		String[] initArray= initialSetting.split(pattern);
		if (initialSetting.charAt(0) == '_') {
			for (int i= 0; i < fieldList.size(); i++) {
				if (fieldList.get(i).charAt(0) == '_') {
					notFlag= true;
					break;
				} else {
					notFlag= false;
				}
			}
			if (!notFlag) {
				return initialSetting.substring(1);
			}
		} else if (initArray.length > 0 && initArray[0].length() == 1) {
			for (int i= 0; i < fieldList.size(); i++) {
				String[] tempList= fieldList.get(i).split(pattern);
				if (tempList.length > 0 && tempList[0].length() == 1) {
					notFlag= true;
					break;
				} else {
					notFlag= false;
				}
			}
			if (!notFlag) {
				if (initialSetting.length() > 2) {
					return initialSetting.substring(1, 2).toLowerCase() + initialSetting.substring(2);
				}
			}
		} else if (initArray.length > 0 && initArray[0].length() != 1) {
			String str= ""; //$NON-NLS-1$
			for (int i= 0; i < fieldList.size(); i++) {
				if (fieldList.get(i).charAt(0) == '_') {
					str = Character.toString(fieldList.get(i).charAt(0));
					return str+initialSetting;
				} else {
					String[] tempList= fieldList.get(i).split(pattern);
					if (tempList.length > 0 && tempList[0].length() == 1) {
						notFlag= false;
						str= Character.toString(tempList[0].charAt(0));
						break;
					} else {
						notFlag= true;
					}
				}
			}
			if (!notFlag && initialSetting.length() > 2) {
					return str + initialSetting.substring(0, 1).toUpperCase() + initialSetting.substring(1);
			}
		}
		return initialSetting;
	}

	public static void getFieldDeclaration(ASTNode cuu, final List<FieldDeclaration> types) {
		cuu.accept(new ASTVisitor() {
			@Override
			public boolean visit(FieldDeclaration node) {
				types.add(node);
				return true;
			}
		});
	}

	public static CompilationUnit getCompilationUnit(String javaFilePath) {
		byte[] input= null;
		try (BufferedInputStream bufferedInputStream= new BufferedInputStream(new FileInputStream(javaFilePath))) {
			input= new byte[bufferedInputStream.available()];
			bufferedInputStream.read(input);
			bufferedInputStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		ASTParser astParser= ASTParser.newParser(AST.JLS20);
		astParser.setKind(ASTParser.K_COMPILATION_UNIT);
		astParser.setSource(new String(input).toCharArray());
		astParser.setResolveBindings(true);
		astParser.setBindingsRecovery(true);
		CompilationUnit unit= (CompilationUnit) (astParser.createAST(null));
		return unit;
	}
}
