/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
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
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.Profile;

public class ModifyDialog extends StatusDialog {
    
    /**
     * The keys to retrieve the preferred width and height from the dialog settings.
     */
    private static final String DS_KEY_PREFERRED_WIDTH = JavaUI.ID_PLUGIN + "formatter_page.modify_dialog.preferred_width"; //$NON-NLS-1$
    private static final String DS_KEY_PREFERRED_HEIGHT = JavaUI.ID_PLUGIN + "formatter_page.modify_dialog.preferred_height"; //$NON-NLS-1$

    /**
     * These are used if no user defined settings are available.
     */
    private final static int PREFERRED_WIDTH_CHARS= 150;
    private final static int PREFERRED_HEIGHT_CHARS= 40;

	
	private final String fTitle;

	private final Profile fProfile;
	private final Map fWorkingValues;
	
	private final IStatus fOkStatus;
	
		
	protected ModifyDialog(Shell parentShell, Profile profile) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX );
		fProfile= profile;
		fWorkingValues= new HashMap(fProfile.getSettings());
		fOkStatus= new Status(IStatus.OK, JavaPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
		updateStatus(fOkStatus);
		fTitle= FormatterMessages.getFormattedString("ModifyDialog.dialog.title", profile.getName()); //$NON-NLS-1$
		setStatusLineAboveButtons(false);
	}
	
	
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(fTitle);
	}

	
	protected void okPressed() {
		fProfile.setSettings(fWorkingValues);
		final Rectangle shell= getShell().getBounds();
		final IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings();
        settings.put(DS_KEY_PREFERRED_WIDTH, shell.width);
        settings.put(DS_KEY_PREFERRED_HEIGHT, shell.height);
	
		super.okPressed();
	}
	
	protected Control createDialogArea(Composite parent) {
		
		GridData gd;
		
		final Composite composite= (Composite)super.createDialogArea(parent);
		
		final TabFolder tabFolder= new TabFolder(composite, SWT.NONE);
		gd= new GridData(GridData.FILL_BOTH);
		tabFolder.setLayoutData(gd);

		addTabPage(tabFolder, FormatterMessages.getString("ModifyDialog.tabpage.braces.title"), new BracesTabPage(this, fWorkingValues)); //$NON-NLS-1$
		addTabPage(tabFolder, FormatterMessages.getString("ModifyDialog.tabpage.indentation.title"), new IndentationTabPage(this, fWorkingValues)); //$NON-NLS-1$
		addTabPage(tabFolder, FormatterMessages.getString("ModifyDialog.tabpage.whitespace.title"), new WhiteSpaceTabPage(this, fWorkingValues)); //$NON-NLS-1$
		addTabPage(tabFolder, FormatterMessages.getString("ModifyDialog.tabpage.blank_lines.title"), new BlankLinesTabPage(this, fWorkingValues)); //$NON-NLS-1$
		addTabPage(tabFolder, FormatterMessages.getString("ModifyDialog.tabpage.new_lines.title"), new NewLinesTabPage(this, fWorkingValues)); //$NON-NLS-1$
		addTabPage(tabFolder, FormatterMessages.getString("ModifyDialog.tabpage.control_statements.title"), new ControlStatementsTabPage(this, fWorkingValues)); //$NON-NLS-1$
		addTabPage(tabFolder, FormatterMessages.getString("ModifyDialog.tabpage.line_wrapping.title"), new LineWrappingTabPage(this, fWorkingValues)); //$NON-NLS-1$
		addTabPage(tabFolder, FormatterMessages.getString("ModifyDialog.tabpage.comments.title"), new CommentsTabPage(this, fWorkingValues)); //$NON-NLS-1$
		addTabPage(tabFolder, FormatterMessages.getString("ModifyDialog.tabpage.other.title"), new OtherSettingsTabPage(this, fWorkingValues)); //$NON-NLS-1$
		
		applyDialogFont(composite);
		
		tabFolder.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}
			public void widgetSelected(SelectionEvent e) {
				final TabItem t= (TabItem)e.item;
				final ModifyDialogTabPage page= (ModifyDialogTabPage)t.getData();
				page.updatePreview();
			}
		});
		
		return composite;
	}
	
	public void updateStatus(IStatus status) {
	    super.updateStatus(status != null ? status : fOkStatus);
	}

    protected void constrainShellSize() {
        
        final IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings();

        final Shell shell= getShell();
        final Rectangle displayBounds= shell.getDisplay().getClientArea();
        
        int preferredWidth;
        int preferredHeight;
        try {
            preferredWidth= settings.getInt(DS_KEY_PREFERRED_WIDTH);
            preferredHeight= settings.getInt(DS_KEY_PREFERRED_HEIGHT);
        } catch (NumberFormatException x) {
            preferredWidth= convertWidthInCharsToPixels(PREFERRED_WIDTH_CHARS);
            preferredHeight= convertHeightInCharsToPixels(PREFERRED_HEIGHT_CHARS);
        }
        
        final Point shellSize= new Point(preferredWidth, preferredHeight);
        
        shellSize.x= Math.min(shellSize.x, displayBounds.width);
        shellSize.y= Math.min(shellSize.y, displayBounds.height);
        
        Point shellLocation= shell.getLocation();

        shellLocation.x= Math.max(0, Math.min(displayBounds.x + displayBounds.width  - shellSize.x, shellLocation.x));
        shellLocation.y= Math.max(0, Math.min(displayBounds.y + displayBounds.height - shellSize.y, shellLocation.y));
        
        shell.setLocation(shellLocation);
        shell.setSize(shellSize);
    }
    
	
	
	private final static void addTabPage(TabFolder tabFolder, String title, ModifyDialogTabPage tabPage) {
		final TabItem tabItem= new TabItem(tabFolder, SWT.NONE);
		applyDialogFont(tabItem.getControl());
		tabItem.setText(title);
		tabItem.setData(tabPage);
		tabItem.setControl(tabPage.createContents(tabFolder));
	}
	
	
}
