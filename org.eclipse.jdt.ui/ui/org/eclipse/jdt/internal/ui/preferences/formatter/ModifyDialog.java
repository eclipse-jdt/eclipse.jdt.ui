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
package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.BuiltInProfile;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.Profile;

public class ModifyDialog extends StatusDialog {
    
    /**
     * The keys to retrieve the preferred area from the dialog settings.
     */
    private static final String DS_KEY_PREFERRED_WIDTH= JavaUI.ID_PLUGIN + "formatter_page.modify_dialog.preferred_width"; //$NON-NLS-1$
    private static final String DS_KEY_PREFERRED_HEIGHT= JavaUI.ID_PLUGIN + "formatter_page.modify_dialog.preferred_height"; //$NON-NLS-1$
    private static final String DS_KEY_PREFERRED_X= JavaUI.ID_PLUGIN + "formatter_page.modify_dialog.preferred_x"; //$NON-NLS-1$
    private static final String DS_KEY_PREFERRED_Y= JavaUI.ID_PLUGIN + "formatter_page.modify_dialog.preferred_y"; //$NON-NLS-1$
    
    
    /**
     * The key to store the number (beginning at 0) of the tab page which had the 
     * focus last time.
     */
    private static final String DS_KEY_LAST_FOCUS= JavaUI.ID_PLUGIN + "formatter_page.modify_dialog.last_focus"; //$NON-NLS-1$ 

	
	private final String fTitle;
	
	private final boolean fNewProfile;

	private final Profile fProfile;
	private final Map fWorkingValues;
	
	private final IStatus fStandardStatus;
	
	protected final List fTabPages;
	
	final IDialogSettings fDialogSettings;
	private TabFolder fTabFolder;
		
	protected ModifyDialog(Shell parentShell, Profile profile, boolean newProfile) {
		super(parentShell);
		fNewProfile= newProfile;
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX );
		
		fProfile= profile;
		if (fProfile instanceof BuiltInProfile) {
		    fStandardStatus= new Status(IStatus.WARNING, JavaPlugin.getPluginId(), IStatus.OK, FormatterMessages.getString("ModifyDialog.dialog.show.warning.builtin"), null); //$NON-NLS-1$
		    fTitle= FormatterMessages.getFormattedString("ModifyDialog.dialog.show.title", profile.getName()); //$NON-NLS-1$
		} else {
		    fStandardStatus= new Status(IStatus.OK, JavaPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
		    fTitle= FormatterMessages.getFormattedString("ModifyDialog.dialog.title", profile.getName()); //$NON-NLS-1$
		}
		fWorkingValues= new HashMap(fProfile.getSettings());
		updateStatus(fStandardStatus);
		setStatusLineAboveButtons(false);
		fTabPages= new ArrayList();
		fDialogSettings= JavaPlugin.getDefault().getDialogSettings();	
	}
	
	public void create() {
		super.create();
		int lastFocusNr= 0;
		try {
			lastFocusNr= fDialogSettings.getInt(DS_KEY_LAST_FOCUS);
			if (lastFocusNr < 0) lastFocusNr= 0;
			if (lastFocusNr > fTabPages.size() - 1) lastFocusNr= fTabPages.size() - 1;
		} catch (NumberFormatException x) {
			lastFocusNr= 0;
		}
		
		if (!fNewProfile) {
			fTabFolder.setSelection(lastFocusNr);
			((ModifyDialogTabPage)fTabFolder.getSelection()[0].getData()).setInitialFocus();
		}
	}
	
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(fTitle);
	}

	protected Control createDialogArea(Composite parent) {
		
		final Composite composite= (Composite)super.createDialogArea(parent);
		
		fTabFolder = new TabFolder(composite, SWT.NONE);
		fTabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

		addTabPage(fTabFolder, FormatterMessages.getString("ModifyDialog.tabpage.indentation.title"), new IndentationTabPage(this, fWorkingValues)); //$NON-NLS-1$
		addTabPage(fTabFolder, FormatterMessages.getString("ModifyDialog.tabpage.braces.title"), new BracesTabPage(this, fWorkingValues)); //$NON-NLS-1$
		addTabPage(fTabFolder, FormatterMessages.getString("ModifyDialog.tabpage.whitespace.title"), new WhiteSpaceTabPage(this, fWorkingValues)); //$NON-NLS-1$
		addTabPage(fTabFolder, FormatterMessages.getString("ModifyDialog.tabpage.blank_lines.title"), new BlankLinesTabPage(this, fWorkingValues)); //$NON-NLS-1$
		addTabPage(fTabFolder, FormatterMessages.getString("ModifyDialog.tabpage.new_lines.title"), new NewLinesTabPage(this, fWorkingValues)); //$NON-NLS-1$
		addTabPage(fTabFolder, FormatterMessages.getString("ModifyDialog.tabpage.control_statements.title"), new ControlStatementsTabPage(this, fWorkingValues)); //$NON-NLS-1$
		addTabPage(fTabFolder, FormatterMessages.getString("ModifyDialog.tabpage.line_wrapping.title"), new LineWrappingTabPage(this, fWorkingValues)); //$NON-NLS-1$
		addTabPage(fTabFolder, FormatterMessages.getString("ModifyDialog.tabpage.comments.title"), new CommentsTabPage(this, fWorkingValues)); //$NON-NLS-1$
		
		applyDialogFont(composite);
		
		fTabFolder.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}
			public void widgetSelected(SelectionEvent e) {
				final TabItem tabItem= (TabItem)e.item;
				final ModifyDialogTabPage page= (ModifyDialogTabPage)tabItem.getData();
//				page.fSashForm.setWeights();
				fDialogSettings.put(DS_KEY_LAST_FOCUS, fTabPages.indexOf(page));
				page.makeVisible();
			}
		});
		return composite;
	}
	
	public void updateStatus(IStatus status) {
	    super.updateStatus(status != null ? status : fStandardStatus);
	}

    protected void constrainShellSize() {
        
        final Shell shell= getShell();
        
        try {
        	final int x= fDialogSettings.getInt(DS_KEY_PREFERRED_X);
        	final int y= fDialogSettings.getInt(DS_KEY_PREFERRED_Y);
        	final int width= fDialogSettings.getInt(DS_KEY_PREFERRED_WIDTH);
            final int height= fDialogSettings.getInt(DS_KEY_PREFERRED_HEIGHT);
            
            shell.setLocation(x, y);
            shell.setSize(width, height);
            
        } catch (NumberFormatException ex) {
        	// there are no values saved, so just leave the defaults
        }

        // make sure we're on the display:
        super.constrainShellSize();
    }
    
	public boolean close()
	{
		fProfile.setSettings(fWorkingValues);
		
		final Rectangle shell= getShell().getBounds();
		
		fDialogSettings.put(DS_KEY_PREFERRED_WIDTH, shell.width);
		fDialogSettings.put(DS_KEY_PREFERRED_HEIGHT, shell.height);
		fDialogSettings.put(DS_KEY_PREFERRED_X, shell.x);
		fDialogSettings.put(DS_KEY_PREFERRED_Y, shell.y);
		
		return super.close();
	}
    
    
	
	private final void addTabPage(TabFolder tabFolder, String title, ModifyDialogTabPage tabPage) {
		final TabItem tabItem= new TabItem(tabFolder, SWT.NONE);
		applyDialogFont(tabItem.getControl());
		tabItem.setText(title);
		tabItem.setData(tabPage);
		tabItem.setControl(tabPage.createContents(tabFolder));
		fTabPages.add(tabPage);
	}
	
	
}
