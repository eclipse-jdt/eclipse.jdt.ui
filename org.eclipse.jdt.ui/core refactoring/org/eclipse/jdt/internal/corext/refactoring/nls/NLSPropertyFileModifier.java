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

import java.util.HashMap;
import java.util.Map;

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
		if (!Checks.resourceExists(propertyFilePath)) {
			textChange= new DocumentChange(name, new Document());
			addChanges(textChange, nlsSubstitutions);
			textChange.perform(new NullProgressMonitor());
			return new CreateTextFileChange(propertyFilePath, textChange.getCurrentContent(new NullProgressMonitor()), "8859_1", "txt"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		textChange= new TextFileChange(name, getPropertyFile(propertyFilePath));

		addChanges(textChange, nlsSubstitutions);

		return textChange;
	}

	private static IFile getPropertyFile(IPath propertyFilePath) {
		return (IFile) (ResourcesPlugin.getWorkspace().getRoot().findMember(propertyFilePath));
	}

	private static void addChanges(TextChange textChange, NLSSubstitution[] substitutions) throws CoreException {
		PropertyFileDocumentModel model= new PropertyFileDocumentModel(textChange.getCurrentDocument(new NullProgressMonitor()));

		HashMap keyToSubstMap= getKeyToSubstitutionMap(substitutions);
		
		addInsertEdits(textChange, substitutions, keyToSubstMap, model);
		addRemoveEdits(textChange, substitutions, keyToSubstMap, model);
		addReplaceEdits(textChange, substitutions, keyToSubstMap, model);
	}

	/**
	 * Maps keys to a substitutions. If a substitution is not in the map then it is a duplicate.
	 */
	private static HashMap getKeyToSubstitutionMap(NLSSubstitution[] substitutions) {
		HashMap keyToSubstMap= new HashMap();
		// find all duplicates
		for (int i= 0; i < substitutions.length; i++) {
			NLSSubstitution curr= substitutions[i];
			if (curr.getState() == NLSSubstitution.EXTERNALIZED) {
				NLSSubstitution val= (NLSSubstitution) keyToSubstMap.get(curr.getKey());
				if (val == null || (val.hasPropertyFileChange() && !curr.hasPropertyFileChange())) {
					keyToSubstMap.put(curr.getKey(), curr); // store if first or if stored in new and we are existing
				}
			}
		}
		return keyToSubstMap;
	}

	private static void addReplaceEdits(TextChange textChange, NLSSubstitution[] substitutions, Map keyToSubstMap, PropertyFileDocumentModel model) {
		for (int i= 0; i < substitutions.length; i++) {
			NLSSubstitution substitution= substitutions[i];
			if (!substitution.hasStateChanged() && (substitution.getState() == NLSSubstitution.EXTERNALIZED)) {
				if (substitution.isKeyRename() || substitution.isValueRename()) {
					if (keyToSubstMap.get(substitution.getKey()) == substitution) { // only rename if we're not a duplicate. duplicates will be removed
						KeyValuePair initialPair= new KeyValuePair(substitution.getInitialKey(), substitution.getInitialValue());
						KeyValuePair newPair= new KeyValuePair(substitution.getKey(), substitution.getValueNonEmpty());
						TextEdit edit= model.replace(initialPair, newPair);
						if (edit != null) {
							TextChangeCompatibility.addTextEdit(textChange, NLSMessages.getFormattedString("NLSPropertyFileModifier.replace_entry", substitution.getKey()), edit); //$NON-NLS-1$
						}
					}
				}
			}
		}
	}

	private static void addInsertEdits(TextChange textChange, NLSSubstitution[] substitutions, Map keyToSubstMap, PropertyFileDocumentModel model) {
		for (int i= 0; i < substitutions.length; i++) {
			NLSSubstitution substitution= substitutions[i];
			boolean isExternalized= substitution.hasStateChanged() && (substitution.getState() == NLSSubstitution.EXTERNALIZED);
			boolean isMissingKey= !substitution.hasStateChanged() && (substitution.getState() == NLSSubstitution.EXTERNALIZED) && (substitution.getInitialValue() == null);
			if (isExternalized || isMissingKey) {
				if (keyToSubstMap.get(substitution.getKey()) == substitution) { // only insert if we're not a duplicate
					String value= substitution.getValueNonEmpty();
				
					KeyValuePair curr= new KeyValuePair(substitution.getKey(), value);
					
					InsertEdit insert= model.insert(curr);
					String message= NLSMessages.getFormattedString("NLSPropertyFileModifier.add_entry", curr.getKey()); //$NON-NLS-1$
					TextChangeCompatibility.addTextEdit(textChange, message, insert);
				}
			}
		}
	}
	
	private static boolean doRemove(NLSSubstitution substitution, Map keyToSubstMap) {
		if (substitution.getInitialState() != NLSSubstitution.EXTERNALIZED || substitution.getInitialValue() == null) {
			return false; // was not in property file before
		}
		if (substitution.hasStateChanged()) {
			return true; // was externalized, but not anymore
		} else {
			if (substitution.hasPropertyFileChange() && keyToSubstMap.get(substitution.getKey()) != substitution) {
				return true; // has been changed to an already existing
			}
		}
		return false;
	}
	

	private static void addRemoveEdits(TextChange textChange, NLSSubstitution[] substitutions, Map keyToSubstMap, PropertyFileDocumentModel model) {
		for (int i= 0; i < substitutions.length; i++) {
			NLSSubstitution substitution= substitutions[i];
			if (doRemove(substitution, keyToSubstMap)) {
				TextEdit edit= model.remove(substitution.getInitialKey());
				TextChangeCompatibility.addTextEdit(textChange, NLSMessages.getFormattedString("NLSPropertyFileModifier.remove_entry", substitution.getInitialKey()), edit); //$NON-NLS-1$
			}
		}
	}

}