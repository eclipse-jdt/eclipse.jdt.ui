/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.ProgressMonitorPart;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class RefactoringWizardDialog2 extends Dialog implements IWizardContainer {

	private RefactoringWizard fWizard;
	private IWizardPage fCurrentPage;
	private IWizardPage fVisiblePage;
	
	private PageBook fPageContainer;
	private PageBook fStatusContainer;
	private MessageBox fMessageBox;
	private ProgressMonitorPart fProgressMonitorPart;
	private int fActiveRunningOperations;
	private Cursor fWaitCursor;
	private Cursor fArrowCursor;

	private static final int PREVIEW_ID= IDialogConstants.CLIENT_ID + 1;

	private int fPreviewWidth;
	private int fPreviewHeight;
	private IDialogSettings fSettings;
	private static final String DIALOG_SETTINGS= "RefactoringWizard.preview"; //$NON-NLS-1$
	private static final String WIDTH= "width"; //$NON-NLS-1$
	private static final String HEIGHT= "height"; //$NON-NLS-1$
	
	private static final Image INFO= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_REFACTORING_INFO);
	private static final Image WARNING= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_REFACTORING_WARNING);
	private static final Image ERROR= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_REFACTORING_ERROR);
	private static final Image FATAL= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_REFACTORING_FATAL);
	
	private static class MessageBox extends Composite {
		GridLayout fLayout;
		Label fImage;
		Label fMessage;
		public MessageBox(Composite parent, int style, int width) {
			super(parent, style);
			fLayout= new GridLayout();
			fLayout.marginHeight= 0;
			fLayout.numColumns= 2;
			setLayout(fLayout);
			fImage= new Label(this, SWT.NONE);
			GridData gd= new GridData();
			gd.verticalAlignment= GridData.BEGINNING;
			Rectangle bounds= INFO.getBounds();
			gd.widthHint= bounds.width; gd.heightHint= bounds.height;
			fImage.setLayoutData(gd);
			fMessage= new Label(this, SWT.LEFT | SWT.WRAP);
			fMessage.setText(" \n "); //$NON-NLS-1$
			gd= new GridData(GridData.FILL_BOTH);
			gd.widthHint= width;
			fMessage.setLayoutData(gd);
		}
		public void setMessage(IWizardPage page) {
			String msg= page.getErrorMessage();
			int type= IMessageProvider.ERROR;
			if (msg == null || msg.length() == 0) {
				msg= page.getMessage();
				type= IMessageProvider.NONE;
			if (msg != null && page instanceof IMessageProvider) 
				type = ((IMessageProvider)page).getMessageType();
			}
			Image image= null;
			switch (type) {
				case IMessageProvider.INFORMATION:
					image= INFO;
					break;
				case IMessageProvider.WARNING:
					image= WARNING;
					break;
				case IMessageProvider.ERROR:
					image= ERROR;
					break;
			}
			if (msg == null)
				msg= ""; //$NON-NLS-1$
			fMessage.setText(msg);
			fImage.setImage(image);
		}
	}
	
	private static class PageBook extends Composite {
		private StackLayout fLayout;
		public PageBook(Composite parent, int style) {
			super(parent, style);
			fLayout= new StackLayout();
			setLayout(fLayout);
			fLayout.marginWidth= 5; fLayout.marginHeight= 5;
		}
		public void showPage(Control page) {
			fLayout.topControl= page;
			layout();
		}
		public Control getTopPage() {
			return fLayout.topControl;
		}
	}
	
	public RefactoringWizardDialog2(Shell shell, RefactoringWizard wizard) {
		super(shell);
		Assert.isNotNull(wizard);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		wizard.setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
		fWizard= wizard; 
		fWizard.setContainer(this);
		fWizard.addPages();
		initSize();
	}
	
	private void initSize() {
		IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings();
		fSettings= settings.getSection(DIALOG_SETTINGS);
		if (fSettings == null) {
			fSettings= new DialogSettings(DIALOG_SETTINGS);
			settings.addSection(fSettings);
			fSettings.put(WIDTH, 600);
			fSettings.put(HEIGHT, 400);
		}
		fPreviewWidth= 600;
		fPreviewHeight= 400;
		try {
			fPreviewWidth= fSettings.getInt(WIDTH);
			fPreviewHeight= fSettings.getInt(HEIGHT);
		} catch (NumberFormatException e) {
		}
	}
	
	private void saveSize() {
		if (fCurrentPage instanceof PreviewWizardPage) {
			Control control= fCurrentPage.getControl().getParent();
			Point size = control.getSize();
			fSettings.put(WIDTH, size.x);
			fSettings.put(HEIGHT, size.y);
		}
	}
	
	//---- IWizardContainer --------------------------------------------

	/* (non-Javadoc)
	 * Method declared on IWizardContainer.
	 */
	public void showPage(IWizardPage page) {
		fCurrentPage= page;
	}

	/* (non-Javadoc)
	 * Method declared on IWizardContainer.
	 */
	public void updateButtons() {
		boolean previewPage= isPreviewPageActive();
		boolean ok= fWizard.canFinish();
		boolean canFlip= fCurrentPage.canFlipToNextPage();
		Button previewButton= getButton(PREVIEW_ID);
		Button defaultButton= null;
		if (previewButton != null && !previewButton.isDisposed()) {
			previewButton.setEnabled(!previewPage);
			if (!previewPage)
				previewButton.setEnabled(canFlip);
			if (previewButton.isEnabled())
				defaultButton= previewButton;
		}
		Button okButton= getButton(IDialogConstants.OK_ID);
		if (okButton != null && !okButton.isDisposed()) {
			okButton.setEnabled(ok);
			if (ok)
				defaultButton= okButton;
		}
		if (defaultButton != null) {
			defaultButton.getShell().setDefaultButton(defaultButton);
		}
	}

	/* (non-Javadoc)
	 * Method declared on IWizardContainer.
	 */
	public void updateMessage() {
		if (fStatusContainer == null || fStatusContainer.isDisposed())
			return;
		fStatusContainer.showPage(fMessageBox);
		fMessageBox.setMessage(fCurrentPage);
	}

	/* (non-Javadoc)
	 * Method declared on IWizardContainer.
	 */
	public void updateTitleBar() {
		// we don't have a title bar.
	}

	/* (non-Javadoc)
	 * Method declared on IWizardContainer.
	 */
	public void updateWindowTitle() {
		getShell().setText(fWizard.getWindowTitle());
	}

	/* (non-Javadoc)
	 * Method declared on IWizardContainer.
	 */
	public IWizardPage getCurrentPage() {
		return fCurrentPage;
	}
	
	//---- IRunnableContext --------------------------------------------

	/* (non-Javadoc)
	 * Method declared on IRunnableContext
	 */
	public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException {
		Object state = null;
		if(fActiveRunningOperations == 0)
			state = aboutToStart(fork && cancelable);
		
		fActiveRunningOperations++;
		try {
			ModalContext.run(runnable, fork, fProgressMonitorPart, getShell().getDisplay());
		} finally {
			fActiveRunningOperations--;
			//Stop if this is the last one
			if(state!= null)
				stopped(state);
		}
	}
	
	private Object aboutToStart(boolean cancelable) {
		Map savedState = null;
		Shell shell= getShell();
		if (shell != null) {
			// Save focus control
			Control focusControl = getShell().getDisplay().getFocusControl();
			if (focusControl != null && focusControl.getShell() != getShell())
				focusControl = null;
				
			Button cancelButton= getButton(IDialogConstants.CANCEL_ID);	
			// Set the busy cursor to all shells.
			Display d = getShell().getDisplay();
			fWaitCursor = new Cursor(d, SWT.CURSOR_WAIT);
			setDisplayCursor(d, fWaitCursor);
					
			// Set the arrow cursor to the cancel component.
			fArrowCursor= new Cursor(d, SWT.CURSOR_ARROW);
			cancelButton.setCursor(fArrowCursor);
	
			// Deactivate shell
			savedState = saveUIState(cancelable);
			if (focusControl != null)
				savedState.put("focus", focusControl); //$NON-NLS-1$
				
			fProgressMonitorPart.attachToCancelComponent(cancelButton);
			fStatusContainer.showPage(fProgressMonitorPart);
		}
		return savedState;
	}
	
	private Map saveUIState(boolean keepCancelEnabled) {
		Map savedState= new HashMap(10);
		saveEnableStateAndSet(getButton(PREVIEW_ID), savedState, "preview", false); //$NON-NLS-1$
		saveEnableStateAndSet(getButton(IDialogConstants.OK_ID), savedState, "ok", false); //$NON-NLS-1$
		saveEnableStateAndSet(getButton(IDialogConstants.CANCEL_ID), savedState, "cancel", keepCancelEnabled); //$NON-NLS-1$
		savedState.put("page", ControlEnableState.disable(fVisiblePage.getControl())); //$NON-NLS-1$
		return savedState;
	}
	
	private void saveEnableStateAndSet(Control w, Map h, String key, boolean enabled) {
		if (w != null) {
			h.put(key, new Boolean(w.isEnabled()));
			w.setEnabled(enabled);
		}
	}
	
	private void setDisplayCursor(Display d, Cursor c) {
		Shell[] shells= d.getShells();
		for (int i= 0; i < shells.length; i++)
			shells[i].setCursor(c);
	}	

	private void stopped(Object savedState) {
		Shell shell= getShell();
		if (shell != null) {
			Button cancelButton= getButton(IDialogConstants.CANCEL_ID);
			
			fProgressMonitorPart.removeFromCancelComponent(cancelButton);
			fStatusContainer.showPage(fMessageBox);
			Map state = (Map)savedState;
			restoreUIState(state);
	
			setDisplayCursor(shell.getDisplay(), null);	
			cancelButton.setCursor(null);
			fWaitCursor.dispose();
			fWaitCursor = null;
			fArrowCursor.dispose();
			fArrowCursor = null;
			Control focusControl = (Control)state.get("focus"); //$NON-NLS-1$
			if (focusControl != null)
				focusControl.setFocus();
		}
	}
	
	private void restoreUIState(Map state) {
		restoreEnableState(getButton(PREVIEW_ID), state, "preview");//$NON-NLS-1$
		restoreEnableState(getButton(IDialogConstants.OK_ID), state, "ok");//$NON-NLS-1$
		restoreEnableState(getButton(IDialogConstants.CANCEL_ID), state, "cancel");//$NON-NLS-1$
		ControlEnableState pageState = (ControlEnableState) state.get("page");//$NON-NLS-1$
		pageState.restore();
	}
	
	private void restoreEnableState(Control w, Map h, String key) {
		if (w != null) {
			Boolean b = (Boolean) h.get(key);
			if (b != null)
				w.setEnabled(b.booleanValue());
		}
	}
	
	//---- Dialog -----------------------------------------------------------
	
	public boolean close() {
		fWizard.dispose();
		return super.close();
	}

	protected void cancelPressed() {
		if (fActiveRunningOperations == 0)	{
			if (fWizard.performCancel())	
				super.cancelPressed();
		}
	}

	protected void okPressed() {
		IWizardPage current= fCurrentPage;
		if (fWizard.performFinish()) {
			saveSize();
			super.okPressed();
			return;
		}
		if (fCurrentPage == current)
			return;
		Assert.isTrue(ErrorWizardPage.PAGE_NAME.equals(fCurrentPage.getName()));
		if (showErrorDialog((ErrorWizardPage)fCurrentPage)) {
			if (fWizard.performFinish()) {
				super.okPressed();
				return;
			}
		}
		fCurrentPage= current;
	}
	
	private boolean isPreviewPageActive() {
		return PreviewWizardPage.PAGE_NAME.equals(fCurrentPage.getName());
	}
	
	private void previewPressed(Button button) {
		IWizardPage current= fCurrentPage;
		fCurrentPage= fCurrentPage.getNextPage();
		if (current == fCurrentPage)
			return;
		String pageName= fCurrentPage.getName();
		if (ErrorWizardPage.PAGE_NAME.equals(pageName)) {
			if (showErrorDialog((ErrorWizardPage)fCurrentPage)) {
				fCurrentPage= fCurrentPage.getNextPage();
				pageName= fCurrentPage.getName();
			} else {
				return;
			}
		}
		if (PreviewWizardPage.PAGE_NAME.equals(pageName)) {
			fCurrentPage.createControl(fPageContainer);
			makeVisible(fCurrentPage);
			updateButtons();
			if (((PreviewWizardPage)fCurrentPage).hasChanges())
				resize();
			else
				getButton(IDialogConstants.OK_ID).setEnabled(false);
		} else {
			fCurrentPage= current;
		}
	}
	
	private boolean showErrorDialog(ErrorWizardPage page) {
		RefactoringStatus status= page.getStatus();
		RefactoringStatusDialog dialog= new RefactoringStatusDialog(getShell(), page);
		switch (dialog.open()) {
			case IDialogConstants.OK_ID:
				return true;
			case IDialogConstants.CANCEL_ID:
				if (status.hasFatalError()) {
					super.cancelPressed();
				} else {
					fCurrentPage= fCurrentPage.getPreviousPage();
				}
		}
		return false;
	}
	
	private void resize() {
		Control control= fPageContainer.getTopPage();
		Point size= control.getSize();
		int dw= Math.max(0, fPreviewWidth - size.x);
		int dh= Math.max(0, fPreviewHeight - size.y);
		int dx = dw / 2;
		int dy= dh / 2;
		Shell shell= getShell();
		Rectangle rect= shell.getBounds();
		Rectangle display= shell.getDisplay().getClientArea();
		rect.x= Math.max(0, rect.x - dx);
		rect.y= Math.max(0, rect.y - dy);
		rect.width= Math.min(rect.width + dw, display.width);
		rect.height= Math.min(rect.height + dh, display.height);
		shell.setBounds(rect);
	}
	
	//---- UI construction ---------------------------------------------------
	
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(fWizard.getPageTitle());
	}
	
	protected Control createContents(Composite parent) {
		Composite result= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0; layout.marginWidth= 0;
		layout.verticalSpacing= 0; layout.horizontalSpacing= 0;
		result.setLayout(layout);
		result.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		// initialize the dialog units
		initializeDialogUnits(result);
	
		fPageContainer= new PageBook(result, SWT.NONE);
		GridData gd= new GridData(GridData.FILL_BOTH);
		fPageContainer.setLayoutData(gd);
		fCurrentPage= fWizard.getStartingPage();
		dialogArea= fPageContainer;
		if (fCurrentPage instanceof PreviewWizardPage) {
			gd.widthHint= fPreviewWidth;
			gd.heightHint= fPreviewHeight;
		}
		
		fStatusContainer= new PageBook(result, SWT.NONE);
		fStatusContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		createProgressMonitorPart();
		createMessageBox();
		fStatusContainer.showPage(fMessageBox);
		
		buttonBar= createButtonBar(result);
		
		fCurrentPage.createControl(fPageContainer);
		makeVisible(fCurrentPage);
				
		updateMessage();
		updateButtons();
		return result;
	}
	
	private void createProgressMonitorPart() {
		// Insert a progress monitor 
		GridLayout pmlayout= new GridLayout();
		pmlayout.numColumns= 1;
		pmlayout.marginHeight= 0;
		fProgressMonitorPart= new ProgressMonitorPart(fStatusContainer, pmlayout);
	}
	
	private void createMessageBox() {
		fMessageBox= new MessageBox(fStatusContainer, SWT.NONE, convertWidthInCharsToPixels(fWizard.getMessageLineWidthInChars()));
	}
	
	protected void createButtonsForButtonBar(Composite parent) {
		if (! (fCurrentPage instanceof PreviewWizardPage)) {
			Button preview= createButton(parent, PREVIEW_ID, RefactoringMessages.getString("RefactoringWizardDialog2.buttons.preview.label"), false); //$NON-NLS-1$
			preview.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					previewPressed((Button)e.widget);
				}
			});
		}
		super.createButtonsForButtonBar(parent);
		Button okButton= getButton(IDialogConstants.OK_ID);
		okButton.setFocus();
	}
	
	private void makeVisible(IWizardPage page) {
		if (fVisiblePage == page)
			return;
		if (fVisiblePage != null)	
			fVisiblePage.setVisible(false);
		fVisiblePage= page;
		fPageContainer.showPage(page.getControl());
		fVisiblePage.setVisible(true);
	}	
}
