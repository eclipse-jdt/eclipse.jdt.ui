/*****************************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the Common Public
 * License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ****************************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.nls;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.InsertEdit;


public class NLSPropertyFileModifier {
	
	public static Change create(
			NLSSubstitution[] nlsSubstitutions,
			IPath propertyFilePath) throws CoreException {
	    
        String name = NLSMessages.getFormattedString("NLSrefactoring.Append_to_property_file", propertyFilePath.toString()); //$NON-NLS-1$
        TextChange textChange = null;
        if (!Checks.resourceExists(propertyFilePath)) {
            // TODO: tmp TextChange Object..stupid
            textChange = new DocumentChange(name, new Document());
            addChanges(textChange, nlsSubstitutions);
            textChange.perform(new NullProgressMonitor());
            return new CreateTextFileChange(propertyFilePath, textChange.getCurrentContent(), "8859_1", "txt"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        textChange = new TextFileChange(name, getPropertyFile(propertyFilePath));

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
        PropertyFileDocumentModell model = new PropertyFileDocumentModell(new Document(textChange.getCurrentContent()));
        addInsertEdits(textChange, substitutions, model);        
        addRemoveEdits(textChange, substitutions, model);        
        addReplaceEdits(textChange, substitutions, model);
    }
    
    private static void addReplaceEdits(TextChange textChange, NLSSubstitution[] substitutions, PropertyFileDocumentModell model) {
        for (int i = 0; i < substitutions.length; i++) {
            NLSSubstitution substitution = substitutions[i];
            if (substitution.hasChanged() && !substitution.hasStateChanged()) {
                if (substitution.getInitialValue() != null) {
                    KeyValuePair initialPair = new KeyValuePair(substitution.getInitialKey(), substitution.getInitialValue());
                    KeyValuePair newPair  = new KeyValuePair(substitution.getKey(), substitution.getValue());
                    TextChangeCompatibility.addTextEdit(textChange, 
                            "NLSRefactoring.replace_entry", 
                            model.replace(initialPair, newPair));
                }
            }
        }
    }

    private static void addInsertEdits(TextChange textChange, NLSSubstitution[] substitutions, PropertyFileDocumentModell modell) {
        KeyValuePair[] keyValuePairs = convertSubstitutionsToKeyValue(substitutions);

        InsertEdit[] inserts = modell.insert(keyValuePairs);
        for (int i = 0; i < inserts.length; i++) {
            String message = NLSMessages.getFormattedString("NLSRefactoring.add_entry", keyValuePairs[i].fKey); //$NON-NLS-1$
            TextChangeCompatibility.addTextEdit(textChange, message, inserts[i]);
        }
        
        for (int i = 0; i < substitutions.length; i++) {
            NLSSubstitution substitution = substitutions[i];
            if (substitution.hasChanged() && !substitution.hasStateChanged() && (substitution.getInitialValue() == null)) {
                InsertEdit insert = modell.insert(new KeyValuePair(substitution.getKey(), substitution.getValue()));
                String message = NLSMessages.getFormattedString("NLSRefactoring.add_entry", substitution.getKey()); //$NON-NLS-1$
                TextChangeCompatibility.addTextEdit(textChange, message, insert);                
            }
        }
    }
 
    private static void addRemoveEdits(TextChange textChange, NLSSubstitution[] substitutions, PropertyFileDocumentModell modell) {
        for (int i = 0; i < substitutions.length; i++) {
            NLSSubstitution substitution = substitutions[i];
            if (substitution.hasStateChanged() && (substitution.getInitialState() == NLSSubstitution.EXTERNALIZED)) {
                if (substitution.getInitialValue() != null) {
                    TextChangeCompatibility.addTextEdit(textChange, "NLSRefactoring.remove_entry", modell.remove(substitution.getKey()));
                }
            }
        }
    }

    private static KeyValuePair[] convertSubstitutionsToKeyValue(NLSSubstitution[] substitutions) {
        List subs = new ArrayList(substitutions.length);
        for (int i = 0; i < substitutions.length; i++) {
            NLSSubstitution substitution = substitutions[i];
            if (substitution.hasStateChanged() && (substitution.getState() == NLSSubstitution.EXTERNALIZED)) {
                subs.add(new KeyValuePair(substitution.getKey(), substitution.getValue()));
            }
        }
        return (KeyValuePair[]) subs.toArray(new KeyValuePair[subs.size()]);
    }
}
