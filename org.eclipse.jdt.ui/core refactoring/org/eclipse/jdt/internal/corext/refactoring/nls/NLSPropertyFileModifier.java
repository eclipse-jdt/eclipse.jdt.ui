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
package org.eclipse.jdt.internal.corext.refactoring.nls;

import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.text.Document;

import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;


public class NLSPropertyFileModifier {

	public static Change create(NLSSubstitution[] nlsSubstitutions, IPath propertyFilePath) throws CoreException {

		String name= NLSMessages.getFormattedString("NLSPropertyFileModifier.change.name", propertyFilePath.toString()); //$NON-NLS-1$
		TextChange textChange= null;
		// TODO: check should not be necessary
		if (!Checks.resourceExists(propertyFilePath)) {
			// TODO: tmp TextChange Object..stupid
			textChange= new DocumentChange(name, new Document());
			addChanges(textChange, nlsSubstitutions);
			textChange.perform(new NullProgressMonitor());
			return new CreateTextFileChange(propertyFilePath, textChange.getCurrentContent(new NullProgressMonitor()), "8859_1", "txt"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		textChange= new TextFileChange(name, getPropertyFile(propertyFilePath));

		try {
			addChanges(textChange, nlsSubstitutions);
		} catch (Exception e) {
			// error while creating some of the changes
		}

		return textChange;
	}

	private static IFile getPropertyFile(IPath propertyFilePath) {
		return (IFile) (ResourcesPlugin.getWorkspace().getRoot().findMember(propertyFilePath));
	}

	private static void addChanges(TextChange textChange, NLSSubstitution[] substitutions) throws CoreException {
		PropertyFileDocumentModel model= new PropertyFileDocumentModel(new Document(textChange.getCurrentContent(new NullProgressMonitor())));
		addInsertEdits(textChange, substitutions, model);
		addRemoveEdits(textChange, substitutions, model);
		addReplaceEdits(textChange, substitutions, model);
	}

	private static void addReplaceEdits(TextChange textChange, NLSSubstitution[] substitutions, PropertyFileDocumentModel model) {
		for (int i= 0; i < substitutions.length; i++) {
			NLSSubstitution substitution= substitutions[i];
			if (!substitution.hasStateChanged() && (substitution.getState() == NLSSubstitution.EXTERNALIZED)) {
				if (substitution.isKeyRename() || substitution.isValueRename()) {
					KeyValuePair initialPair= new KeyValuePair(substitution.getInitialKey(), substitution.getInitialValue());
					KeyValuePair newPair= new KeyValuePair(substitution.getKey(), substitution.getValue());
					TextEdit edit= model.replace(initialPair, newPair);
					if (edit != null) {
						TextChangeCompatibility.addTextEdit(textChange, NLSMessages.getFormattedString("NLSPropertyFileModifier.replace_entry", substitution.getKey()), edit); //$NON-NLS-1$
					}
				}
			}
		}
	}

	private static void addInsertEdits(TextChange textChange, NLSSubstitution[] substitutions, PropertyFileDocumentModel model) {
		for (int i= 0; i < substitutions.length; i++) {
			NLSSubstitution substitution= substitutions[i];
			boolean isExternalized= substitution.hasStateChanged() && (substitution.getState() == NLSSubstitution.EXTERNALIZED);
			boolean isMissingKey= !substitution.hasStateChanged() && (substitution.getState() == NLSSubstitution.EXTERNALIZED) && (substitution.getInitialValue() == null);
			if (isExternalized || isMissingKey) {
				String value= substitution.getValue();
				if (value == null) {
					value= ""; //$NON-NLS-1$
				}
			
				KeyValuePair curr= new KeyValuePair(substitution.getKey(), value);
				
				InsertEdit insert= model.insert(curr);
				String message= NLSMessages.getFormattedString("NLSPropertyFileModifier.add_entry", curr.getKey()); //$NON-NLS-1$
				TextChangeCompatibility.addTextEdit(textChange, message, insert);
			}
		}
	}

	private static void addRemoveEdits(TextChange textChange, NLSSubstitution[] substitutions, PropertyFileDocumentModel model) {
		for (int i= 0; i < substitutions.length; i++) {
			NLSSubstitution substitution= substitutions[i];
			if (substitution.hasStateChanged() && (substitution.getInitialState() == NLSSubstitution.EXTERNALIZED)) {
				if (substitution.getInitialValue() != null) {
					TextEdit edit= model.remove(substitution.getKey());
					TextChangeCompatibility.addTextEdit(textChange, NLSMessages.getFormattedString("NLSPropertyFileModifier.remove_entry", substitution.getKey()), edit); //$NON-NLS-1$
				}
			}
		}
	}

}