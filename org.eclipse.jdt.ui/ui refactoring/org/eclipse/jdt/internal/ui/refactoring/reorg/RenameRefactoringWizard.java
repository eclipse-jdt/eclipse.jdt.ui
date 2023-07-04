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
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
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

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.NamingConventions;
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
	static List<FieldDeclaration> fieldDeclarations = new ArrayList<>();
	static boolean flag= false;
	static String fieldDeclaration;
	// dialog settings constants:

	/**
	 * Dialog settings key (value is of type boolean).
	 */
	public static final String UPDATE_TEXTUAL_MATCHES = "updateTextualMatches"; //$NON-NLS-1$
	/**
	 * Dialog settings key (value is of type boolean).
	 */
	public static final String UPDATE_QUALIFIED_NAMES = "updateQualifiedNames"; //$NON-NLS-1$
	/**
	 * Dialog settings key (value is of type String).
	 */
	public static final String QUALIFIED_NAMES_PATTERNS = "patterns"; //$NON-NLS-1$

	/**
	 * Dialog settings key (value is of type boolean).
	 */
	public static final String TYPE_UPDATE_SIMILAR_ELEMENTS = "updateSimilarElements"; //$NON-NLS-1$
	/**
	 * Dialog settings key (value is of type int).
	 *
	 * @see RenamingNameSuggestor
	 */
	public static final String TYPE_SIMILAR_MATCH_STRATEGY = "updateSimilarElementsMatchStrategy"; //$NON-NLS-1$

	/**
	 * Dialog settings key (value is of type boolean).
	 */
	public static final String PACKAGE_RENAME_SUBPACKAGES = "renameSubpackages"; //$NON-NLS-1$

	/**
	 * Dialog settings key (value is of type boolean).
	 */
	public static final String FIELD_RENAME_GETTER = "renameGetter"; //$NON-NLS-1$
	/**
	 * Dialog settings key (value is of type boolean).
	 */
	public static final String FIELD_RENAME_SETTER = "renameSetter"; //$NON-NLS-1$

	public RenameRefactoringWizard(Refactoring refactoring, String defaultPageTitle, String inputPageDescription,
			ImageDescriptor inputPageImageDescriptor, String pageContextHelpId) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE);
		setDefaultPageTitle(defaultPageTitle);
		fInputPageDescription = inputPageDescription;
		fInputPageImageDescriptor = inputPageImageDescriptor;
		fPageContextHelpId = pageContextHelpId;
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
		INameUpdating ref = getNameUpdating();
		ref.setNewElementName(newName);
		try {
			return ref.checkNewElementName(newName);
		} catch (CoreException e) {
			JavaPlugin.log(e);
			return RefactoringStatus.createFatalErrorStatus(RefactoringMessages.RenameRefactoringWizard_internal_error);
		}
	}

    public static String suggestedFieldName(String initialSetting) {
    	IEditorPart activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
    	IEditorInput editorInput = activeEditor.getEditorInput();
    	IFile file = editorInput.getAdapter(IFile.class);
    	String filePath = file.getLocation().toOSString();
    	IJavaProject project= JavaCore.create(file.getProject());
    	CompilationUnit cu= getCompilationUnit(filePath);
		int modifier= 0;
    	if(!fieldDeclarations.isEmpty()) {fieldDeclarations.clear();}
        getFieldDeclaration(cu,fieldDeclarations);
    	String[] excluded= new String[fieldDeclarations.size()];
        for(int i=0;i<fieldDeclarations.size();i++) {
        	FieldDeclaration fd = fieldDeclarations.get(i);
			VariableDeclarationFragment vdf = (VariableDeclarationFragment) fd.fragments().get(0);
			String fieldName = vdf.getName().getIdentifier();
			if(initialSetting.equals(fieldName)) {
				flag= true;
				modifier= fd.getModifiers();
				fieldDeclaration= fd.toString();
			}else {
				excluded[i]= fieldName;
			}
        }
        if (flag && fieldDeclaration.contains("static") && fieldDeclaration.contains("final")) { //$NON-NLS-1$ //$NON-NLS-2$
			String[] newNames= getFieldNameSuggestions(project, initialSetting, modifier, excluded);
			if (newNames.length > 0) {
				return newNames[0];
			}
		}
        return initialSetting;
    }
	public static String[] getFieldNameSuggestions(IJavaProject project, String originalField, int fieldModifiers, String[] excluded) {
		return getFieldNameSuggestions(project, originalField, 0, fieldModifiers, excluded);
	}

	public static String[] getFieldNameSuggestions(IJavaProject project, String baseName, int dimensions, int modifiers, String[] excluded) {
		if (Flags.isFinal(modifiers) && Flags.isStatic(modifiers)) {
			return getVariableNameSuggestions(NamingConventions.VK_STATIC_FINAL_FIELD, project, baseName, dimensions, new ExcludedCollection(excluded), true);
		} else if (Flags.isStatic(modifiers)) {
			return getVariableNameSuggestions(NamingConventions.VK_STATIC_FIELD, project, baseName, dimensions, new ExcludedCollection(excluded), true);
		}
		return getVariableNameSuggestions(NamingConventions.VK_INSTANCE_FIELD, project, baseName, dimensions, new ExcludedCollection(excluded), true);
	}

	public static String[] getVariableNameSuggestions(int variableKind, IJavaProject project, String baseName, int dimensions, Collection<String> excluded, boolean evaluateDefault) {
		return NamingConventions.suggestVariableNames(variableKind, NamingConventions.BK_TYPE_NAME, removeTypeArguments(baseName), project, dimensions, getExcludedArray(excluded), evaluateDefault);
	}

	private static String removeTypeArguments(String baseName) {
		int idx= baseName.indexOf('<');
		if (idx != -1) {
			return baseName.substring(0, idx);
		}
		return baseName;
	}

	private static String[] getExcludedArray(Collection<String> excluded) {
		if (excluded == null) {
			return null;
		} else if (excluded instanceof ExcludedCollection) {
			return ((ExcludedCollection) excluded).getExcludedArray();
		}
		return excluded.toArray(new String[excluded.size()]);
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
	private static class ExcludedCollection extends AbstractList<String> {
		private String[] fExcluded;

		public ExcludedCollection(String[] excluded) {
			fExcluded= excluded;
		}

		public String[] getExcludedArray() {
			return fExcluded;
		}

		@Override
		public int size() {
			return fExcluded.length;
		}

		@Override
		public String get(int index) {
			return fExcluded[index];
		}

		@Override
		public int indexOf(Object o) {
			if (o instanceof String) {
				for (int i= 0; i < fExcluded.length; i++) {
					if (o.equals(fExcluded[i]))
						return i;
				}
			}
			return -1;
		}

		@Override
		public boolean contains(Object o) {
			return indexOf(o) != -1;
		}
		@SuppressWarnings("unused")
		public static void test(String[] args) {
			List<Person> list= new ArrayList<Person>();

			Iterator<Person> iterPeople= list.iterator();

			while (iterPeople.hasNext()) {
				System.out.println(iterPeople.next().getName() + " " + iterPeople.next().getAge()); //$NON-NLS-1$
			}
		}
	}
	class Person {
		public Person(String name, int age) {
			this.name= name;
			this.age= age;
		}

		String name;

		int age;

		public String getName() {
			return name;
		}

		public int getAge() {
			return age;
		}
	}
}
