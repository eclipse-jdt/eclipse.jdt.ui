/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Aaron Luchko, aluchko@redhat.com - 105926 [Formatter] Exporting Unnamed profile fails silently
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.util.List;

import org.eclipse.core.runtime.preferences.IScopeContext;

import org.eclipse.core.resources.IProject;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.preferences.PreferencesAccess;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.Profile;



/**
 * The code formatter preference page. 
 */

public class CodeFormatterConfigurationBlock extends ProfileConfigurationBlock {
    
    private static final String FORMATTER_DIALOG_PREFERENCE_KEY= "formatter_page"; //$NON-NLS-1$

	private static final String DIALOGSTORE_LASTSAVELOADPATH= JavaUI.ID_PLUGIN + ".codeformatter"; //$NON-NLS-1$
    
	/**
     * Some Java source code used for preview.
     */
    protected static final String PREVIEW= "/**\n* " + //$NON-NLS-1$
    		FormatterMessages.CodingStyleConfigurationBlock_preview_title + 
    		"\n*/\n\n" + //$NON-NLS-1$
    		"package mypackage; import java.util.LinkedList; public class MyIntStack {" + //$NON-NLS-1$
    		"private final LinkedList fStack;" + //$NON-NLS-1$
    		"public MyIntStack(){fStack= new LinkedList();}" + //$NON-NLS-1$
    		"public int pop(){return ((Integer)fStack.removeFirst()).intValue();}" + //$NON-NLS-1$
    		"public void push(int elem){fStack.addFirst(new Integer(elem));}" + //$NON-NLS-1$
    		"public boolean isEmpty() {return fStack.isEmpty();}" + //$NON-NLS-1$
    		"}"; //$NON-NLS-1$

	/**
	 * Create a new <code>CodeFormatterConfigurationBlock</code>.
	 */
	public CodeFormatterConfigurationBlock(IProject project, PreferencesAccess access) {
		super(project, access, DIALOGSTORE_LASTSAVELOADPATH);
	}

	protected IProfileVersioner createProfileVersioner() {
	    return new ProfileVersioner();
    }
	
	protected ProfileStore createProfileStore(IProfileVersioner versioner) {
	    return new FormatterProfileStore(versioner);
    }
	
	protected ProfileManager createProfileManager(List profiles, IScopeContext context, PreferencesAccess access, IProfileVersioner profileVersioner) {
	    return new FormatterProfileManager(profiles, context, access, profileVersioner);
    }
	
	protected JavaPreview createJavaPreview(Composite composite, int numColumns, Profile profile) {
    	CompilationUnitPreview result= new CompilationUnitPreview(profile.getSettings(), composite);
		result.setPreviewText(PREVIEW);
		return result;
    }
    
    protected ModifyDialog createModifyDialog(Shell shell, Profile profile, ProfileManager profileManager, boolean newProfile) {
        return new FormatterModifyDialog(shell, profile, profileManager, newProfile, FORMATTER_DIALOG_PREFERENCE_KEY);
    }
}
