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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
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
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility2;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.util.ViewerPane;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

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

	private static class OverrideMethodLabelProvider extends LabelProvider {

		private static int computeJavaAdornmentFlags(IBinding binding, int flags) {
			int adornments= 0;
			if (binding instanceof ITypeBinding || binding instanceof IMethodBinding) {

				if (binding instanceof IMethodBinding && ((IMethodBinding) binding).isConstructor())
					adornments|= JavaElementImageDescriptor.CONSTRUCTOR;

				int modifiers= binding.getModifiers();
				if (Modifier.isAbstract(modifiers) && isAbstract(binding))
					adornments|= JavaElementImageDescriptor.ABSTRACT;
				if (Modifier.isFinal(modifiers) || isInterfaceField(binding))
					adornments|= JavaElementImageDescriptor.FINAL;
				if (Modifier.isSynchronized(modifiers) && isSynchronized(binding))
					adornments|= JavaElementImageDescriptor.SYNCHRONIZED;
				if (Modifier.isStatic(modifiers) || isInterfaceField(binding))
					adornments|= JavaElementImageDescriptor.STATIC;

				if (binding.isDeprecated())
					adornments|= JavaElementImageDescriptor.DEPRECATED;
			}
			return adornments;
		}

		private static ImageDescriptor getBaseImageDescriptor(IBinding binding, int flags) {
			if (binding instanceof ITypeBinding)
				return getTypeImageDescriptor((ITypeBinding) binding, flags);
			else if (binding instanceof IMethodBinding) {
				ITypeBinding type= ((IMethodBinding) binding).getDeclaringClass();
				return getMethodImageDescriptor(type.isAnnotation() || type.isInterface(), binding.getModifiers());
			}
			return JavaPluginImages.DESC_OBJS_UNKNOWN;
		}

		private static ImageDescriptor getClassImageDescriptor(int modifiers) {
			if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers) || Modifier.isPrivate(modifiers))
				return JavaPluginImages.DESC_OBJS_CLASS;
			else
				return JavaPluginImages.DESC_OBJS_CLASS_DEFAULT;
		}

		private static boolean getFlag(long flags, long flag) {
			return (flags & flag) != 0;
		}

		private static ImageDescriptor getInnerClassImageDescriptor(boolean isInInterface, int modifiers) {
			if (Modifier.isPublic(modifiers) || isInInterface)
				return JavaPluginImages.DESC_OBJS_INNER_CLASS_PUBLIC;
			else if (Modifier.isPrivate(modifiers))
				return JavaPluginImages.DESC_OBJS_INNER_CLASS_PRIVATE;
			else if (Modifier.isProtected(modifiers))
				return JavaPluginImages.DESC_OBJS_INNER_CLASS_PROTECTED;
			else
				return JavaPluginImages.DESC_OBJS_INNER_CLASS_DEFAULT;
		}

		private static ImageDescriptor getInnerInterfaceImageDescriptor(boolean isInInterface, int modifiers) {
			if (Modifier.isPublic(modifiers) || isInInterface)
				return JavaPluginImages.DESC_OBJS_INNER_INTERFACE_PUBLIC;
			else if (Modifier.isPrivate(modifiers))
				return JavaPluginImages.DESC_OBJS_INNER_INTERFACE_PRIVATE;
			else if (Modifier.isProtected(modifiers))
				return JavaPluginImages.DESC_OBJS_INNER_INTERFACE_PROTECTED;
			else
				return JavaPluginImages.DESC_OBJS_INTERFACE_DEFAULT;
		}

		private static ImageDescriptor getInterfaceImageDescriptor(int modifiers) {
			if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers) || Modifier.isPrivate(modifiers))
				return JavaPluginImages.DESC_OBJS_INTERFACE;
			else
				return JavaPluginImages.DESC_OBJS_INTERFACE_DEFAULT;
		}

		public static ImageDescriptor getJavaImageDescriptor(IBinding binding, int flags) {
			return new JavaElementImageDescriptor(getBaseImageDescriptor(binding, flags), computeJavaAdornmentFlags(binding, flags), useSmallSize(flags) ? JavaElementImageProvider.SMALL_SIZE : JavaElementImageProvider.BIG_SIZE);
		}

		private static ImageDescriptor getMethodImageDescriptor(boolean deferred, int modifiers) {
			if (Modifier.isPublic(modifiers) || deferred)
				return JavaPluginImages.DESC_MISC_PUBLIC;
			if (Modifier.isProtected(modifiers))
				return JavaPluginImages.DESC_MISC_PROTECTED;
			if (Modifier.isPrivate(modifiers))
				return JavaPluginImages.DESC_MISC_PRIVATE;

			return JavaPluginImages.DESC_MISC_DEFAULT;
		}

		private static void getMethodLabel(IMethodBinding binding, long flags, StringBuffer buffer) {
			// return type
			if (getFlag(flags, JavaElementLabels.M_PRE_TYPE_PARAMETERS)) {
				ITypeBinding[] parameters= binding.getTypeParameters();
				if (parameters.length > 0) {
					buffer.append('<');
					for (int i= 0; i < parameters.length; i++) {
						if (i > 0) {
							buffer.append(JavaElementLabels.COMMA_STRING);
						}
						buffer.append(parameters[i].getName());
					}
					buffer.append('>');
					buffer.append(' ');
				}
			}
			// return type
			if (getFlag(flags, JavaElementLabels.M_PRE_RETURNTYPE) && !binding.isConstructor()) {
				buffer.append(binding.getReturnType().getName());
				buffer.append(' ');
			}
			// qualification
			if (getFlag(flags, JavaElementLabels.M_FULLY_QUALIFIED)) {
				getTypeLabel(binding.getDeclaringClass(), JavaElementLabels.T_FULLY_QUALIFIED | (flags & JavaElementLabels.P_COMPRESSED), buffer);
				buffer.append('.');
			}
			buffer.append(binding.getName());
			// parameters
			buffer.append('(');
			if (getFlag(flags, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES)) {
				ITypeBinding[] arguments= getFlag(flags, JavaElementLabels.M_PARAMETER_TYPES) ? binding.getParameterTypes() : null;
				if (arguments != null) {
					for (int index= 0; index < arguments.length; index++) {
						if (index > 0) {
							buffer.append(JavaElementLabels.COMMA_STRING); //$NON-NLS-1$
						}
						if (arguments != null) {
							if (binding.isVarargs() && (index == arguments.length - 1)) {
								int dimension= arguments[index].getDimensions() - 1;
								if (dimension >= 0)
									buffer.append(arguments[index].getElementType().getName());
								else
									buffer.append(arguments[index].getName());
								for (int offset= 0; offset < dimension; offset++) {
									buffer.append("[]"); //$NON-NLS-1$
								}
								buffer.append("..."); //$NON-NLS-1$
							} else {
								buffer.append(arguments[index].getName());
							}
						}
					}
				}
			} else {
				if (binding.getParameterTypes().length > 0) {
					buffer.append("..."); //$NON-NLS-1$
				}
			}
			buffer.append(')');
			if (getFlag(flags, JavaElementLabels.M_EXCEPTIONS)) {
				ITypeBinding[] exceptions= binding.getExceptionTypes();
				if (exceptions.length > 0) {
					buffer.append(" throws "); //$NON-NLS-1$
					for (int index= 0; index < exceptions.length; index++) {
						if (index > 0) {
							buffer.append(JavaElementLabels.COMMA_STRING);
						}
						buffer.append(exceptions[index].getName());
					}
				}
			}
			if (getFlag(flags, JavaElementLabels.M_APP_TYPE_PARAMETERS)) {
				ITypeBinding[] parameters= binding.getTypeParameters();
				if (parameters.length > 0) {
					buffer.append(' ');
					buffer.append('<');
					for (int index= 0; index < parameters.length; index++) {
						if (index > 0) {
							buffer.append(JavaElementLabels.COMMA_STRING);
						}
						buffer.append(parameters[index].getName());
					}
					buffer.append('>');
				}
			}
			if (getFlag(flags, JavaElementLabels.M_APP_RETURNTYPE) && !binding.isConstructor()) {
				buffer.append(JavaElementLabels.DECL_STRING);
				buffer.append(binding.getReturnType().getName());
			}
			// post qualification
			if (getFlag(flags, JavaElementLabels.M_POST_QUALIFIED)) {
				buffer.append(JavaElementLabels.CONCAT_STRING);
				getTypeLabel(binding.getDeclaringClass(), JavaElementLabels.T_FULLY_QUALIFIED | (flags & JavaElementLabels.P_COMPRESSED), buffer);
			}
		}

		private static ImageDescriptor getTypeImageDescriptor(boolean isInner, boolean isInInterface, ITypeBinding binding, int flags) {
			if (binding.isEnum()) {
				return JavaPluginImages.DESC_OBJS_ENUM;
			} else if (binding.isAnnotation()) {
				return JavaPluginImages.DESC_OBJS_ANNOTATION;
			} else if (binding.isInterface()) {
				if (useLightIcons(flags)) {
					return JavaPluginImages.DESC_OBJS_INTERFACEALT;
				}
				if (isInner) {
					return getInnerInterfaceImageDescriptor(isInInterface, binding.getModifiers());
				}
				return getInterfaceImageDescriptor(binding.getModifiers());
			} else {
				if (useLightIcons(flags)) {
					return JavaPluginImages.DESC_OBJS_CLASSALT;
				}
				if (isInner) {
					return getInnerClassImageDescriptor(isInInterface, binding.getModifiers());
				}
				return getClassImageDescriptor(binding.getModifiers());
			}
		}

		private static ImageDescriptor getTypeImageDescriptor(ITypeBinding binding, int flags) {
			ITypeBinding declaring= binding.getDeclaringClass();
			boolean isInner= declaring != null;
			boolean isInInterface= isInner && (declaring.isInterface() || declaring.isAnnotation());
			return getTypeImageDescriptor(isInner, isInInterface, binding, flags);
		}

		private static void getTypeLabel(ITypeBinding binding, long flags, StringBuffer buffer) {
			String name= binding.getName();
			if (name.length() == 0) { // anonymous
				if (binding.isEnum()) {
					name= "{...}"; //$NON-NLS-1$
				} else {
					ITypeBinding ancestor= binding.getSuperclass();
					if (ancestor != null) {
						name= JavaUIMessages.getFormattedString("JavaElementLabels.anonym_type", ancestor.getName()); //$NON-NLS-1$
					}
				}
				if (name == null || name.length() == 0)
					name= JavaUIMessages.getString("JavaElementLabels.anonym"); //$NON-NLS-1$
			}
			buffer.append(name);
		}

		private static boolean isAbstract(IBinding binding) {
			if (binding instanceof ITypeBinding) {
				ITypeBinding type= (ITypeBinding) binding;
				return !type.isInterface() && !type.isAnnotation();
			}
			if (binding instanceof IMethodBinding) {
				ITypeBinding type= ((IMethodBinding) binding).getDeclaringClass();
				return !type.isInterface() && !type.isAnnotation();
			} else if (binding instanceof IVariableBinding) {
				ITypeBinding type= ((IVariableBinding) binding).getDeclaringClass();
				return !type.isInterface() && !type.isAnnotation();
			}
			return false;
		}

		private static boolean isInterfaceField(IBinding binding) {
			if (binding instanceof IVariableBinding) {
				ITypeBinding type= (ITypeBinding) binding;
				return type.isInterface() || type.isAnnotation();
			}
			return false;
		}

		private static boolean isSynchronized(IBinding binding) {
			return binding instanceof ITypeBinding;
		}

		private static boolean useLightIcons(int flags) {
			return (flags & JavaElementImageProvider.LIGHT_TYPE_ICONS) != 0;
		}

		private static boolean useSmallSize(int flags) {
			return (flags & JavaElementImageProvider.SMALL_ICONS) != 0;
		}

		private ImageDescriptorRegistry fRegistry= null;

		/*
		 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			if (element instanceof ITypeBinding) {
				return getImageLabel(getJavaImageDescriptor(((ITypeBinding) element), JavaElementLabelProvider.SHOW_DEFAULT));
			} else if (element instanceof IMethodBinding) {
				return getImageLabel(getJavaImageDescriptor(((IMethodBinding) element), JavaElementLabelProvider.SHOW_DEFAULT));
			}
			return null;
		}

		private Image getImageLabel(ImageDescriptor descriptor) {
			if (descriptor == null)
				return null;
			return getRegistry().get(descriptor);
		}

		private ImageDescriptorRegistry getRegistry() {
			if (fRegistry == null) {
				fRegistry= JavaPlugin.getImageDescriptorRegistry();
			}
			return fRegistry;
		}

		/*
		 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			if (element instanceof ITypeBinding) {
				StringBuffer buffer= new StringBuffer();
				getTypeLabel(((ITypeBinding) element), 0, buffer);
				return buffer.toString();
			} else if (element instanceof IMethodBinding) {
				StringBuffer buffer= new StringBuffer();
				getMethodLabel(((IMethodBinding) element), JavaElementLabels.M_PARAMETER_TYPES, buffer);
				return buffer.toString();
			}
			return null;
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
		super(parent, new OverrideMethodLabelProvider(), new OverrideMethodContentProvider(), editor, type, false);
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