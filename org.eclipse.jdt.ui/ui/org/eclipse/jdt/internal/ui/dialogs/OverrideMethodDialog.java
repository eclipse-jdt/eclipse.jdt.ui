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
package org.eclipse.jdt.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.internal.dialogs.ContainerCheckedTreeViewer;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility2;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;


import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.util.ViewerPane;

public class OverrideMethodDialog extends SourceActionDialog {

	private class OverrideFlatTreeAction extends Action {

		private boolean fToggle;

		public OverrideFlatTreeAction() {
			setToolTipText(JavaUIMessages.getString("OverrideMethodDialog.groupMethodsByTypes")); //$NON-NLS-1$

			JavaPluginImages.setLocalImageDescriptors(this, "impl_co.gif"); //$NON-NLS-1$

			fToggle= getOverrideContentProvider().isShowTypes();
			setChecked(fToggle);
		}

		private OverrideMethodContentProvider getOverrideContentProvider() {
			return (OverrideMethodContentProvider) getContentProvider();
		}

		public void run() {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=39264
			Object[] elementList= getOverrideContentProvider().getViewer().getCheckedElements();
			fToggle= !fToggle;
			setChecked(fToggle);
			getOverrideContentProvider().setShowTypes(fToggle);
			getOverrideContentProvider().getViewer().setCheckedElements(elementList);
		}

	}

	private static class OverrideMethodContentProvider implements ITreeContentProvider {

		private final Object[] fEmpty= new Object[0];

		private IMethodBinding[] fMethods;

		private IDialogSettings fSettings;

		private boolean fShowTypes;

		private Object[] fTypes;

		private ContainerCheckedTreeViewer fViewer;

		private final String SETTINGS_SECTION= "OverrideMethodDialog"; //$NON-NLS-1$

		private final String SETTINGS_SHOWTYPES= "showtypes"; //$NON-NLS-1$

		/**
		 * Constructor for OverrideMethodContentProvider.
		 */
		public OverrideMethodContentProvider() {
			IDialogSettings dialogSettings= JavaPlugin.getDefault().getDialogSettings();
			fSettings= dialogSettings.getSection(SETTINGS_SECTION);
			if (fSettings == null) {
				fSettings= dialogSettings.addNewSection(SETTINGS_SECTION);
				fSettings.put(SETTINGS_SHOWTYPES, true);
			}
			fShowTypes= fSettings.getBoolean(SETTINGS_SHOWTYPES);
		}

		/*
		 * @see IContentProvider#dispose()
		 */
		public void dispose() {
		}

		/*
		 * @see ITreeContentProvider#getChildren(Object)
		 */
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof ITypeBinding) {
				ArrayList result= new ArrayList(fMethods.length);
				for (int index= 0; index < fMethods.length; index++) {
					if (fMethods[index].getDeclaringClass().isEqualTo((IBinding) parentElement))
						result.add(fMethods[index]);
				}
				return result.toArray();
			}
			return fEmpty;
		}

		/*
		 * @see IStructuredContentProvider#getElements(Object)
		 */
		public Object[] getElements(Object inputElement) {
			return fShowTypes ? fTypes : fMethods;
		}

		/*
		 * @see ITreeContentProvider#getParent(Object)
		 */
		public Object getParent(Object element) {
			if (element instanceof IMethodBinding) {
				return ((IMethodBinding) element).getDeclaringClass();
			}
			return null;
		}

		public ContainerCheckedTreeViewer getViewer() {
			return fViewer;
		}

		/*
		 * @see ITreeContentProvider#hasChildren(Object)
		 */
		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}

		public void init(IMethodBinding[] methods, ITypeBinding[] types) {
			fMethods= methods;
			fTypes= types;
		}

		/*
		 * @see IContentProvider#inputChanged(Viewer, Object, Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			fViewer= (ContainerCheckedTreeViewer) viewer;
		}

		public boolean isShowTypes() {
			return fShowTypes;
		}

		public void setShowTypes(boolean showTypes) {
			if (fShowTypes != showTypes) {
				fShowTypes= showTypes;
				fSettings.put(SETTINGS_SHOWTYPES, showTypes);
				if (fViewer != null)
					fViewer.refresh();
			}
		}
	}

	private static class OverrideMethodSorter extends ViewerSorter {

		private ITypeBinding[] fAllTypes;

		public OverrideMethodSorter(ITypeBinding curr) {
			ITypeBinding[] superTypes= Bindings.getAllSuperTypes(curr);
			fAllTypes= new ITypeBinding[superTypes.length + 1];
			fAllTypes[0]= curr;
			System.arraycopy(superTypes, 0, fAllTypes, 1, superTypes.length);
		}

		/*
		 * @see ViewerSorter#compare(Viewer, Object, Object)
		 */
		public int compare(Viewer viewer, Object first, Object second) {
			if (first instanceof ITypeBinding && second instanceof ITypeBinding) {
				if (((IBinding) first).isEqualTo((IBinding) second))
					return 0;
				for (int i= 0; i < fAllTypes.length; i++) {
					if (fAllTypes[i].isEqualTo((IBinding) first))
						return -1;
					if (fAllTypes[i].isEqualTo((IBinding) second))
						return 1;
				}
				return 0;
			} else
				return super.compare(viewer, first, second);
		}
	}

	private static class OverrideMethodValidator implements ISelectionStatusValidator {

		private static int fNumMethods;

		public OverrideMethodValidator(int entries) {
			fNumMethods= entries;
		}

		/*
		 * @see ISelectionValidator#validate(Object[])
		 */
		public IStatus validate(Object[] selection) {
			int count= 0;
			for (int index= 0; index < selection.length; index++) {
				if (selection[index] instanceof IMethodBinding)
					count++;
			}
			if (count == 0)
				return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
			return new StatusInfo(IStatus.INFO, JavaUIMessages.getFormattedString("OverrideMethodDialog.selectioninfo.more", new String[] { String.valueOf(count), String.valueOf(fNumMethods)})); //$NON-NLS-1$
		}
	}

	public OverrideMethodDialog(Shell parent, CompilationUnitEditor editor, IType type, boolean isSubType) throws JavaModelException {
		super(parent, new SourceActionLabelProvider(), new OverrideMethodContentProvider(), editor, type, false);
		RefactoringASTParser parser= new RefactoringASTParser(AST.JLS3);
		CompilationUnit unit= parser.parse(type.getCompilationUnit(), true);
		ITypeBinding binding= null;
		if (type.isAnonymous()) {
			final ClassInstanceCreation creation= ASTNodeSearchUtil.getClassInstanceCreationNode(type, unit);
			if (creation != null)
				binding= creation.resolveTypeBinding();
		} else {
			final AbstractTypeDeclaration declaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(type, unit);
			if (declaration != null)
				binding= declaration.resolveBinding();
		}
		IMethodBinding[] overridable= StubUtility2.getOverridableMethods(binding, false);

		List toImplement= new ArrayList();
		for (int i= 0; i < overridable.length; i++) {
			if (Modifier.isAbstract(overridable[i].getModifiers())) {
				toImplement.add(overridable[i]);
			}
		}
		IMethodBinding[] toImplementArray= (IMethodBinding[]) toImplement.toArray(new IMethodBinding[toImplement.size()]);
		setInitialSelections(toImplementArray);

		HashSet expanded= new HashSet(toImplementArray.length);
		for (int i= 0; i < toImplementArray.length; i++) {
			expanded.add(toImplementArray[i].getDeclaringClass());
		}

		HashSet types= new HashSet(overridable.length);
		for (int i= 0; i < overridable.length; i++) {
			types.add(overridable[i].getDeclaringClass());
		}

		ITypeBinding[] typesArrays= (ITypeBinding[]) types.toArray(new ITypeBinding[types.size()]);
		ViewerSorter sorter= new OverrideMethodSorter(binding);
		if (expanded.isEmpty() && typesArrays.length > 0) {
			sorter.sort(null, typesArrays);
			expanded.add(typesArrays[0]);
		}
		setExpandedElements(expanded.toArray());

		((OverrideMethodContentProvider) getContentProvider()).init(overridable, typesArrays);

		setTitle(JavaUIMessages.getString("OverrideMethodDialog.dialog.title")); //$NON-NLS-1$
		setMessage(null);
		setValidator(new OverrideMethodValidator(overridable.length));
		setSorter(sorter);
		setContainerMode(true);
		setSize(60, 18);
		setInput(new Object());
	}

	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		WorkbenchHelp.setHelp(newShell, IJavaHelpContextIds.OVERRIDE_TREE_SELECTION_DIALOG);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.dialogs.SourceActionDialog#createLinkControl(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createLinkControl(Composite composite) {
		final Control control= createLinkText(composite, new Object[] { JavaUIMessages.getString("OverrideMethodDialog.link.text.before"), new String[] { JavaUIMessages.getString("OverrideMethodDialog.link.text.middle"), "org.eclipse.jdt.ui.preferences.CodeTemplatePreferencePage", "overridecomment", JavaUIMessages.getString("OverrideMethodDialog.link.tooltip")}, JavaUIMessages.getString("OverrideMethodDialog.link.text.after")}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		final GridData data= new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		data.widthHint= 150; // only expand further if anyone else requires it
		control.setLayoutData(data);
		return control;
	}

	/*
	 * @see CheckedTreeSelectionDialog#createTreeViewer(Composite)
	 */
	protected CheckboxTreeViewer createTreeViewer(Composite composite) {
		initializeDialogUnits(composite);
		ViewerPane pane= new ViewerPane(composite, SWT.BORDER | SWT.FLAT);
		pane.setText(JavaUIMessages.getString("OverrideMethodDialog.dialog.description")); //$NON-NLS-1$
		CheckboxTreeViewer treeViewer= super.createTreeViewer(pane);
		pane.setContent(treeViewer.getControl());
		GridLayout paneLayout= new GridLayout();
		paneLayout.marginHeight= 0;
		paneLayout.marginWidth= 0;
		paneLayout.numColumns= 1;
		pane.setLayout(paneLayout);
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= convertWidthInCharsToPixels(55);
		gd.heightHint= convertHeightInCharsToPixels(15);
		pane.setLayoutData(gd);
		ToolBarManager manager= pane.getToolBarManager();
		manager.add(new OverrideFlatTreeAction()); // create after tree is created
		manager.update(true);
		treeViewer.getTree().setFocus();
		return treeViewer;
	}

	public boolean hasMethodsToOverride() {
		return getContentProvider().getElements(null).length > 0;
	}

	protected void createAnnotationControls(Composite composite) {
		Composite annotationComposite= createAnnotationSelection(composite);
		annotationComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));		
	}
}