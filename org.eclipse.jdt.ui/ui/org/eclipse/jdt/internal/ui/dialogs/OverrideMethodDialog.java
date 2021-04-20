/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility2Core;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.util.PatternMatcher;
import org.eclipse.jdt.internal.ui.util.ViewerPane;
import org.eclipse.jdt.internal.ui.viewsupport.BindingLabelProvider;

public class OverrideMethodDialog extends SourceActionDialog {

	private class OverrideFlatTreeAction extends Action {

		private boolean fToggle;

		public OverrideFlatTreeAction() {
			setToolTipText(JavaUIMessages.OverrideMethodDialog_groupMethodsByTypes);

			JavaPluginImages.setLocalImageDescriptors(this, "impl_co.png"); //$NON-NLS-1$

			fToggle= getOverrideContentProvider().isShowTypes();
			setChecked(fToggle);
		}

		private OverrideMethodContentProvider getOverrideContentProvider() {
			return (OverrideMethodContentProvider) getContentProvider();
		}

		@Override
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
		@Override
		public void dispose() {
		}

		/*
		 * @see ITreeContentProvider#getChildren(Object)
		 */
		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof ITypeBinding) {
				ArrayList<IMethodBinding> result= new ArrayList<>(fMethods.length);
				for (IMethodBinding fMethod : fMethods) {
					if (fMethod.getDeclaringClass().isEqualTo((IBinding) parentElement)) {
						result.add(fMethod);
					}
				}
				return result.toArray();
			}
			return fEmpty;
		}

		/*
		 * @see IStructuredContentProvider#getElements(Object)
		 */
		@Override
		public Object[] getElements(Object inputElement) {
			return fShowTypes ? fTypes : fMethods;
		}

		/*
		 * @see ITreeContentProvider#getParent(Object)
		 */
		@Override
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
		@Override
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
		@Override
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

	private static class OverrideMethodComparator extends ViewerComparator {

		private ITypeBinding[] fAllTypes= new ITypeBinding[0];

		public OverrideMethodComparator(ITypeBinding curr) {
			if (curr != null) {
				ITypeBinding[] superTypes= Bindings.getAllSuperTypes(curr);
				fAllTypes= new ITypeBinding[superTypes.length + 1];
				fAllTypes[0]= curr;
				System.arraycopy(superTypes, 0, fAllTypes, 1, superTypes.length);
			}
		}

		/*
		 * @see ViewerSorter#compare(Viewer, Object, Object)
		 */
		@Override
		public int compare(Viewer viewer, Object first, Object second) {
			if (first instanceof ITypeBinding && second instanceof ITypeBinding) {
				final ITypeBinding left= (ITypeBinding) first;
				final ITypeBinding right= (ITypeBinding) second;
				if ("java.lang.Object".equals(right.getQualifiedName())) //$NON-NLS-1$
					return -1;
				if (left.isEqualTo(right))
					return 0;
				if (Bindings.isSuperType(left, right))
					return +1;
				else if (Bindings.isSuperType(right, left))
					return -1;
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
		@Override
		public IStatus validate(Object[] selection) {
			int count= 0;
			for (Object s : selection) {
				if (s instanceof IMethodBinding) {
					count++;
				}
			}
			if (count == 0)
				return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
			return new StatusInfo(IStatus.INFO, Messages.format(JavaUIMessages.OverrideMethodDialog_selectioninfo_more, new String[] { String.valueOf(count), String.valueOf(fNumMethods)}));
		}
	}

	private static ITypeBinding getSuperType(final ITypeBinding binding, final String name) {

		if (binding.isArray() || binding.isPrimitive())
			return null;

		if (binding.getQualifiedName().startsWith(name))
			return binding;

		final ITypeBinding type= binding.getSuperclass();
		if (type != null) {
			final ITypeBinding result= getSuperType(type, name);
			if (result != null)
				return result;
		}
		for (ITypeBinding t : binding.getInterfaces()) {
			final ITypeBinding result= getSuperType(t, name);
			if (result != null)
				return result;
		}
		return null;
	}

	private CompilationUnit fUnit= null;

	public OverrideMethodDialog(Shell shell, CompilationUnitEditor editor, IType type, boolean isSubType) throws JavaModelException {
		super(shell, new BindingLabelProvider(), new OverrideMethodContentProvider(), editor, type, false);
		RefactoringASTParser parser= new RefactoringASTParser(IASTSharedValues.SHARED_AST_LEVEL);
		fUnit= parser.parse(type.getCompilationUnit(), true);
		final ITypeBinding binding= ASTNodes.getTypeBinding(fUnit, type);
		List<IMethodBinding> toImplement= new ArrayList<>();
		IMethodBinding[] overridable= null;
		if (binding != null) {
			final IPackageBinding pack= binding.getPackage();
			final IMethodBinding[] methods= StubUtility2Core.getOverridableMethods(fUnit.getAST(), binding, false);
			List<IMethodBinding> list= new ArrayList<>(methods.length);
			for (IMethodBinding cur : methods) {
				if (Bindings.isVisibleInHierarchy(cur, pack))
					list.add(cur);
			}
			overridable= list.toArray(new IMethodBinding[list.size()]);
		} else
			overridable= new IMethodBinding[] {};
		for (IMethodBinding b : overridable) {
			if (Modifier.isAbstract(b.getModifiers())) {
				toImplement.add(b);
			}
		}

		if (binding != null) {
			ITypeBinding cloneable= getSuperType(binding, "java.lang.Cloneable"); //$NON-NLS-1$
			if (cloneable != null) {
				for (IMethodBinding method : fUnit.getAST().resolveWellKnownType("java.lang.Object").getDeclaredMethods()) { //$NON-NLS-1$
					if ("clone".equals(method.getName()) && method.getParameterTypes().length == 0) //$NON-NLS-1$
						toImplement.add(method);
				}
			}
		}

		IMethodBinding[] toImplementArray= toImplement.toArray(new IMethodBinding[toImplement.size()]);
		setInitialSelections((Object[]) toImplementArray);

		HashSet<ITypeBinding> expanded= new HashSet<>(toImplementArray.length);
		for (IMethodBinding b : toImplementArray) {
			expanded.add(b.getDeclaringClass());
		}

		HashSet<ITypeBinding> types= new HashSet<>(overridable.length);
		for (IMethodBinding b : overridable) {
			types.add(b.getDeclaringClass());
		}

		ITypeBinding[] typesArrays= types.toArray(new ITypeBinding[types.size()]);
		OverrideMethodComparator comparator= new OverrideMethodComparator(binding);
		if (expanded.isEmpty() && typesArrays.length > 0) {
			comparator.sort(null, typesArrays);
			expanded.add(typesArrays[0]);
		}
		setExpandedElements(expanded.toArray());

		((OverrideMethodContentProvider) getContentProvider()).init(overridable, typesArrays);

		setTitle(JavaUIMessages.OverrideMethodDialog_dialog_title);
		setMessage(null);
		setValidator(new OverrideMethodValidator(overridable.length));
		setComparator(comparator);
		setContainerMode(true);
		setSize(60, 18);
		setInput(new Object());
	}

	public CompilationUnit getCompilationUnit() {
		return fUnit;
	}

	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.OVERRIDE_TREE_SELECTION_DIALOG);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.dialogs.SourceActionDialog#createLinkControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createLinkControl(Composite composite) {
		Link link= new Link(composite, SWT.WRAP);
		link.setText(JavaUIMessages.OverrideMethodDialog_link_message);
		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openCodeTempatePage(CodeTemplateContextType.OVERRIDECOMMENT_ID);
			}
		});
		link.setToolTipText(JavaUIMessages.OverrideMethodDialog_link_tooltip);

		GridData gridData= new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		gridData.widthHint= convertWidthInCharsToPixels(40); // only expand further if anyone else requires it
		link.setLayoutData(gridData);
		return link;
	}

	/*
	 * @see CheckedTreeSelectionDialog#createTreeViewer(Composite)
	 */
	@Override
	protected CheckboxTreeViewer createTreeViewer(Composite composite) {
		initializeDialogUnits(composite);
		ViewerPane pane= new ViewerPane(composite, SWT.BORDER | SWT.FLAT);
		pane.setText(JavaUIMessages.OverrideMethodDialog_dialog_description);
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

	@Override
	protected Text createFilterComposite(Composite inner) {
		Label filterTextLabel = new Label(inner, SWT.NONE);
		filterTextLabel.setText(JavaUIMessages.OverrideMethodDialog_filter_description);
		Text filterText= new Text(inner, SWT.SEARCH | SWT.BORDER);
		filterText.setMessage(JavaUIMessages.OverrideMethodDialog_searchtext_message);
		return filterText;
	}

	public boolean hasMethodsToOverride() {
		return getContentProvider().getElements(null).length > 0;
	}

	@Override
	protected void addMethodSearchFilter(Text filterText, CheckboxTreeViewer treeViewer) {

		filterText.addModifyListener(e -> {
			final String searchText = filterText.getText();

			PatternMatcher matcher = new PatternMatcher(searchText);
			ViewerFilter vf = null;
			if(!searchText.trim().isEmpty()) {
				vf = new ViewerFilter() {

					@Override
					public boolean select(Viewer viewer, Object parentElement, Object element) {
						IBaseLabelProvider lblProvider= getTreeViewer().getLabelProvider();
						if(element instanceof ITypeBinding) {
							return true;
						}

						String filterableName = null;
						if(lblProvider instanceof LabelProvider) {
							filterableName = ((LabelProvider)lblProvider).getText(element);
							return matcher.matches(filterableName);
						}

						return false;
					}
				};
				treeViewer.setFilters(vf);
				treeViewer.expandAll();
			} else {
				treeViewer.resetFilters();
			}
		});
	}

}
