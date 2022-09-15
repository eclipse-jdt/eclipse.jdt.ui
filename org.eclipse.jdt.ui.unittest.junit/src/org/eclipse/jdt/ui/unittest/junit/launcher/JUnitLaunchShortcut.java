/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     David Saff (saff@mit.edu) - bug 102632: [JUnit] Support for JUnit 4.
 *******************************************************************************/
package org.eclipse.jdt.ui.unittest.junit.launcher;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.unittest.ui.ConfigureViewerSupport;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;

import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.ILaunchShortcut2;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOrdinaryClassFile;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.junit.launcher.AssertionVMArg;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.internal.junit.launcher.JUnitMigrationDelegate;
import org.eclipse.jdt.internal.junit.util.ExceptionHandler;
import org.eclipse.jdt.internal.junit.util.JUnitStubUtility;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.unittest.junit.JUnitTestPlugin;

import org.eclipse.jdt.internal.ui.actions.SelectionConverter;

/**
 * The launch shortcut to launch JUnit tests.
 *
 * <p>
 * This class may be instantiated and subclassed.
 * </p>
 */
public class JUnitLaunchShortcut implements ILaunchShortcut2 {

	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	// see org.junit.runner.Description.METHOD_AND_CLASS_NAME_PATTERN
	private static final Pattern METHOD_AND_CLASS_NAME_PATTERN = Pattern.compile("(.*)\\((.*)\\)"); //$NON-NLS-1$

	/**
	 * Default constructor.
	 */
	public JUnitLaunchShortcut() {
	}

	@Override
	public void launch(IEditorPart editor, String mode) {
		ITypeRoot element = JavaUI.getEditorInputTypeRoot(editor.getEditorInput());
		if (element != null) {
			IMember selectedMember = resolveSelectedMemberName(editor, element);
			if (selectedMember != null) {
				launch(new Object[] { selectedMember }, mode);
			} else {
				launch(new Object[] { element }, mode);
			}
		} else {
			showNoTestsFoundDialog();
		}
	}

	private IMember resolveSelectedMemberName(IEditorPart editor, ITypeRoot element) {
		try {
			ISelectionProvider selectionProvider = editor.getSite().getSelectionProvider();
			if (selectionProvider == null)
				return null;

			ISelection selection = selectionProvider.getSelection();
			if (!(selection instanceof ITextSelection))
				return null;

			ITextSelection textSelection = (ITextSelection) selection;

			IJavaElement elementAtOffset = SelectionConverter.getElementAtOffset(element, textSelection);
			if (!(elementAtOffset instanceof IMethod) && !(elementAtOffset instanceof IType))
				return null;

			IMember member = (IMember) elementAtOffset;

			ISourceRange nameRange = member.getNameRange();
			if (nameRange.getOffset() <= textSelection.getOffset() && textSelection.getOffset()
					+ textSelection.getLength() <= nameRange.getOffset() + nameRange.getLength())
				return member;
		} catch (JavaModelException e) {
			// ignore
		}
		return null;
	}

	@Override
	public void launch(ISelection selection, String mode) {
		if (selection instanceof IStructuredSelection) {
			launch(((IStructuredSelection) selection).toArray(), mode);
		} else {
			showNoTestsFoundDialog();
		}
	}

	private void launch(Object[] elements, String mode) {
		try {
			IJavaElement elementToLaunch = null;

			if (elements.length == 1) {
				Object selected = elements[0];
				if (!(selected instanceof IJavaElement) && selected instanceof IAdaptable) {
					selected = ((IAdaptable) selected).getAdapter(IJavaElement.class);
				}
				if (selected instanceof IJavaElement) {
					IJavaElement element = (IJavaElement) selected;
					switch (element.getElementType()) {
					case IJavaElement.JAVA_PROJECT:
					case IJavaElement.PACKAGE_FRAGMENT_ROOT:
					case IJavaElement.PACKAGE_FRAGMENT:
					case IJavaElement.TYPE:
					case IJavaElement.METHOD:
						elementToLaunch = element;
						break;
					case IJavaElement.CLASS_FILE:
						if (element instanceof IOrdinaryClassFile)
							elementToLaunch = ((IOrdinaryClassFile) element).getType();
						break;
					case IJavaElement.COMPILATION_UNIT:
						elementToLaunch = findTypeToLaunch((ICompilationUnit) element, mode);
						break;
					}
				}
			}
			if (elementToLaunch == null) {
				showNoTestsFoundDialog();
				return;
			}
			performLaunch(elementToLaunch, mode);
		} catch (InterruptedException e) {
			// OK, silently move on
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), Messages.UnitTestLaunchShortcut_dialog_title,
					Messages.UnitTestLaunchShortcut_message_launchfailed);
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(), Messages.UnitTestLaunchShortcut_dialog_title,
					Messages.UnitTestLaunchShortcut_message_launchfailed);
		}
	}

	private void showNoTestsFoundDialog() {
		MessageDialog.openInformation(getShell(), Messages.UnitTestLaunchShortcut_dialog_title,
				Messages.UnitTestLaunchShortcut_message_notests);
	}

	private IType findTypeToLaunch(ICompilationUnit cu, String mode)
			throws InterruptedException, InvocationTargetException {
		var types= findTypesToLaunch(cu);
		if (types.isEmpty()) {
			return null;
		} else if (types.size() > 1) {
			return chooseType(types, mode);
		}
		return types.iterator().next();
	}

	private Set<IType> findTypesToLaunch(ICompilationUnit cu) throws InterruptedException, InvocationTargetException {
		return TestSearchEngine.findTests(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), cu,
				JUnitTestPlugin.getJUnitVersion(cu).getJUnitTestKind());
	}

	private void performLaunch(IJavaElement element, String mode) throws InterruptedException, CoreException {
		ILaunchConfigurationWorkingCopy temparary = createLaunchConfiguration(element);
		ILaunchConfiguration config = findExistingLaunchConfiguration(temparary, mode);
		if (config == null) {
			// no existing found: create a new one
			config = temparary.doSave();
		}
		DebugUITools.launch(config, mode);
	}

	static class TreeProvider implements ITreeContentProvider {
		private final static Object ROOT= new Object();

		private final Map<Object, List<IType>> tree= new HashMap<>();

		public TreeProvider(Set<IType> types) {
			for (var type : types) {
				var parent= type.getParent();
				var parentInTree= types.contains(parent) ? parent : ROOT;
				tree.compute(parentInTree, (key, value) -> {
					var list= value != null ? value : new ArrayList<IType>();
					list.add(type);
					return list;
				});
			}
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return tree.get(ROOT).toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			var children= tree.get(parentElement);
			return children != null ? children.toArray() : new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			var children= tree.get(element);
			return children != null && !children.isEmpty();
		}
	}

	private IType chooseType(Set<IType> types, String mode) throws InterruptedException {
		var dialog = new ElementTreeSelectionDialog(getShell(), new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_POST_QUALIFIED), new TreeProvider(types)) {
			@Override
			protected TreeViewer createTreeViewer(Composite parent) {
				var tree = super.createTreeViewer(parent);
				tree.expandAll();
				return tree;
			}
		};
		dialog.setTitle(Messages.UnitTestLaunchShortcut_dialog_title2);
		dialog.setAllowMultiple(false);
		dialog.setInput(TreeProvider.ROOT);
		if (ILaunchManager.DEBUG_MODE.equals(mode)) {
			dialog.setMessage(Messages.UnitTestLaunchShortcut_message_selectTestToDebug);
		} else {
			dialog.setMessage(Messages.UnitTestLaunchShortcut_message_selectTestToRun);
		}
		if (dialog.open() == Window.OK) {
			return (IType) dialog.getFirstResult();
		}
		throw new InterruptedException(); // cancelled by user
	}

	private Shell getShell() {
		return JUnitTestPlugin.getActiveWorkbenchShell();
	}

	private ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
	}

	/**
	 * Show a selection dialog that allows the user to choose one of the specified
	 * launch configurations. Return the chosen config, or <code>null</code> if the
	 * user cancelled the dialog.
	 *
	 * @param configList list of {@link ILaunchConfiguration}s
	 * @param mode       launch mode
	 * @return ILaunchConfiguration
	 * @throws InterruptedException if cancelled by the user
	 */
	private ILaunchConfiguration chooseConfiguration(List<ILaunchConfiguration> configList, String mode)
			throws InterruptedException {
		IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), labelProvider);
		dialog.setElements(configList.toArray());
		dialog.setTitle(Messages.UnitTestLaunchShortcut_message_selectConfiguration);
		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			dialog.setMessage(Messages.UnitTestLaunchShortcut_message_selectDebugConfiguration);
		} else {
			dialog.setMessage(Messages.UnitTestLaunchShortcut_message_selectRunConfiguration);
		}
		dialog.setMultipleSelection(false);
		int result = dialog.open();
		if (result == Window.OK) {
			return (ILaunchConfiguration) dialog.getFirstResult();
		}
		throw new InterruptedException(); // cancelled by user
	}

	/**
	 * Returns the launch configuration type id of the launch configuration this
	 * shortcut will create. Clients can override this method to return the id of
	 * their launch configuration.
	 *
	 * @return the launch configuration type id of the launch configuration this
	 *         shortcut will create
	 */
	protected String getLaunchConfigurationTypeId() {
		// must match extension in plugin.xml
		return JUnitTestPlugin.PLUGIN_ID + ".launchConfiguration"; //$NON-NLS-1$
	}

	/**
	 * Creates a launch configuration working copy for the given element. The launch
	 * configuration type created will be of the type returned by
	 * {@link #getLaunchConfigurationTypeId}. The element type can only be of type
	 * {@link IJavaProject}, {@link IPackageFragmentRoot}, {@link IPackageFragment},
	 * {@link IType} or {@link IMethod}.
	 *
	 * <p>
	 * Clients can extend this method (should call super) to configure additional
	 * attributes on the launch configuration working copy. Note that this method
	 * calls
	 * <code>{@link #createLaunchConfiguration(IJavaElement, String) createLaunchConfiguration}(element, null)</code>.
	 * Extenders are recommended to extend the two-args method instead of this
	 * method.
	 * </p>
	 *
	 * @param element element to launch
	 *
	 * @return a launch configuration working copy for the given element
	 * @throws CoreException if creation failed
	 */
	protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(IJavaElement element) throws CoreException {
		return createLaunchConfiguration(element, null);
	}

	/**
	 * Creates a launch configuration working copy for the given element. The launch
	 * configuration type created will be of the type returned by
	 * {@link #getLaunchConfigurationTypeId}. The element type can only be of type
	 * {@link IJavaProject}, {@link IPackageFragmentRoot}, {@link IPackageFragment},
	 * {@link IType} or {@link IMethod}.
	 *
	 * <p>
	 * Clients can extend this method (should call super) to configure additional
	 * attributes on the launch configuration working copy.
	 * </p>
	 *
	 * @param element  element to launch
	 * @param testName name of the test to launch, e.g. the method name or an
	 *                 artificial name created by a JUnit runner, or
	 *                 <code>null</code> if none. The testName is ignored if the
	 *                 element is an IMethod; the method name is used in that case.
	 *
	 * @return a launch configuration working copy for the given element
	 * @throws CoreException if creation failed
	 */
	protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(IJavaElement element, String testName)
			throws CoreException {
		final String mainTypeQualifiedName;
		final String containerHandleId;

		switch (element.getElementType()) {
		case IJavaElement.JAVA_PROJECT:
		case IJavaElement.PACKAGE_FRAGMENT_ROOT:
		case IJavaElement.PACKAGE_FRAGMENT: {
			containerHandleId = element.getHandleIdentifier();
			mainTypeQualifiedName = EMPTY_STRING;
			break;
		}
		case IJavaElement.TYPE: {
			containerHandleId = EMPTY_STRING;
			mainTypeQualifiedName = ((IType) element).getFullyQualifiedName('.'); // don't replace, fix for binary inner
																					// types
			break;
		}
		case IJavaElement.METHOD: {
			IMethod method = (IMethod) element;
			testName = method.getElementName(); // Test-names can not be specified when launching a Java method.
			testName += JUnitStubUtility.getParameterTypes(method, false);
			containerHandleId = EMPTY_STRING;
			IType declaringType = method.getDeclaringType();
			mainTypeQualifiedName = declaringType.getFullyQualifiedName('.');
			break;
		}
		default:
			throw new IllegalArgumentException(
					"Invalid element type to create a launch configuration: " + element.getClass().getName()); //$NON-NLS-1$
		}

		ILaunchConfigurationType configType = getLaunchManager()
				.getLaunchConfigurationType(getLaunchConfigurationTypeId());
		String configName = getLaunchManager()
				.generateLaunchConfigurationName(suggestLaunchConfigurationName(element, testName));
		ILaunchConfigurationWorkingCopy wc = configType.newInstance(null, configName);

		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, mainTypeQualifiedName);
		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, element.getJavaProject().getElementName());
		/*
		 * As AbstractJavaLaunchConfigurationDelegate.getVMSpecificAttributesMap(
		 * ILaunchConfiguration) method works with Java Launch, it requires
		 * `IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME` to be set, while we
		 * operate only with `UnitTestLaunchConfigurationConstants.ATTR_PROJECT_NAME`.
		 * So, copy our project name property to `'JavaLaunch...`-one in order tp
		 * satisfy `getVMSpecificAttributesMap()`
		 */
		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, element.getJavaProject().getElementName());

		wc.setAttribute(JUnitLaunchConfigurationConstants.ATTR_KEEPRUNNING, false);
		wc.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER, containerHandleId);
		wc.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_RUNNER_KIND,
				JUnitTestPlugin.getJUnitVersion(element).getJUnitTestKind().getId());
		new ConfigureViewerSupport(JUnitTestPlugin.UNIT_TEST_VIEW_SUPPORT_ID).apply(wc);
		JUnitMigrationDelegate.mapResources(wc);
		AssertionVMArg.setArgDefault(wc);
		if (testName != null) {
			wc.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_NAME, testName);
		}
		boolean isRunWithJUnitPlatform = JUnitTestPlugin.isRunWithJUnitPlatform(element);
		if (isRunWithJUnitPlatform) {
			wc.setAttribute(JUnitLaunchConfigurationConstants.ATTR_RUN_WITH_JUNIT_PLATFORM_ANNOTATION, true);
		}
		return wc;
	}

	/**
	 * Computes a human-readable name for a launch configuration. The name serves as
	 * a suggestion and it's the caller's responsibility to make it valid and
	 * unique.
	 *
	 * @param element      The Java Element that will be executed.
	 * @param fullTestName The test name. See
	 *                     org.eclipse.jdt.internal.junit4.runner.DescriptionMatcher
	 *                     for supported formats.
	 * @return The suggested name for the launch configuration.
	 */
	protected String suggestLaunchConfigurationName(IJavaElement element, String fullTestName) {
		switch (element.getElementType()) {
		case IJavaElement.JAVA_PROJECT:
		case IJavaElement.PACKAGE_FRAGMENT_ROOT:
		case IJavaElement.PACKAGE_FRAGMENT:
			String name = JavaElementLabels.getTextLabel(element, JavaElementLabels.ALL_FULLY_QUALIFIED);
			return name.substring(name.lastIndexOf(IPath.SEPARATOR) + 1);
		case IJavaElement.TYPE:
			if (fullTestName != null) {
				Matcher matcher = METHOD_AND_CLASS_NAME_PATTERN.matcher(fullTestName);
				if (matcher.matches()) {
					String typeFQN = matcher.group(2);
					String testName = matcher.group(1);
					int typeFQNDot = typeFQN.lastIndexOf('.');
					String typeName = typeFQNDot >= 0 ? typeFQN.substring(typeFQNDot + 1) : typeFQN;
					return typeName + " " + testName; //$NON-NLS-1$
				}
				return element.getElementName() + " " + fullTestName;//$NON-NLS-1$
			}
			return element.getElementName();
		case IJavaElement.METHOD:
			IMethod method = (IMethod) element;
			String methodName = method.getElementName();
			methodName += JUnitStubUtility.getParameterTypes(method, true); // use simple names of parameter types
			return method.getDeclaringType().getElementName() + '.' + methodName;
		default:
			throw new IllegalArgumentException(
					"Invalid element type to create a launch configuration: " + element.getClass().getName()); //$NON-NLS-1$
		}
	}

	/**
	 * Returns the attribute names of the attributes that are compared when looking
	 * for an existing similar launch configuration. Clients can override and
	 * replace to customize.
	 *
	 * @return the attribute names of the attributes that are compared
	 */
	protected String[] getAttributeNamesToCompare() {
		return new String[] { IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, //
				JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER, //
				IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, //
				JUnitLaunchConfigurationConstants.ATTR_TEST_NAME };
	}

	private static boolean hasSameAttributes(ILaunchConfiguration config1, ILaunchConfiguration config2,
			String[] attributeToCompare) {
		try {
			for (String element : attributeToCompare) {
				String val1 = config1.getAttribute(element, EMPTY_STRING);
				String val2 = config2.getAttribute(element, EMPTY_STRING);
				if (!val1.equals(val2)) {
					return false;
				}
			}
			return true;
		} catch (CoreException e) {
			// ignore access problems here, return false
		}
		return false;
	}

	private ILaunchConfiguration findExistingLaunchConfiguration(ILaunchConfigurationWorkingCopy temporary, String mode)
			throws InterruptedException, CoreException {
		List<ILaunchConfiguration> candidateConfigs = findExistingLaunchConfigurations(temporary);

		// If there are no existing configs associated with the IType, create
		// one.
		// If there is exactly one config associated with the IType, return it.
		// Otherwise, if there is more than one config associated with the
		// IType, prompt the
		// user to choose one.
		int candidateCount = candidateConfigs.size();
		switch (candidateCount) {
		case 0:
			return null;
		case 1:
			return candidateConfigs.get(0);
		default:
			// Prompt the user to choose a config. A null result means the user
			// cancelled the dialog, in which case this method returns null,
			// since cancelling the dialog should also cancel launching
			// anything.
			ILaunchConfiguration config = chooseConfiguration(candidateConfigs, mode);
			if (config != null) {
				return config;
			}
			break;
		}
		return null;
	}

	private List<ILaunchConfiguration> findExistingLaunchConfigurations(ILaunchConfigurationWorkingCopy temporary)
			throws CoreException {
		ILaunchConfigurationType configType = temporary.getType();

		ILaunchConfiguration[] configs = getLaunchManager().getLaunchConfigurations(configType);
		String[] attributeToCompare = getAttributeNamesToCompare();

		ArrayList<ILaunchConfiguration> candidateConfigs = new ArrayList<>(configs.length);
		for (ILaunchConfiguration config : configs) {
			if (hasSameAttributes(config, temporary, attributeToCompare)) {
				candidateConfigs.add(config);
			}
		}
		return candidateConfigs;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ILaunchConfiguration[] getLaunchConfigurations(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			if (ss.size() == 1) {
				return findExistingLaunchConfigurations(ss.getFirstElement());
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ILaunchConfiguration[] getLaunchConfigurations(final IEditorPart editor) {
		final ITypeRoot element = JavaUI.getEditorInputTypeRoot(editor.getEditorInput());
		if (element != null) {
			IMember selectedMember = null;
			if (Display.getCurrent() == null) {
				final AtomicReference<IMember> temp = new AtomicReference<>();
				Runnable runnable = () -> temp.set(resolveSelectedMemberName(editor, element));
				Display.getDefault().syncExec(runnable);
				selectedMember = temp.get();
			} else {
				selectedMember = resolveSelectedMemberName(editor, element);
			}
			Object candidate = element;
			if (selectedMember != null) {
				candidate = selectedMember;
			}
			return findExistingLaunchConfigurations(candidate);
		}
		return null;
	}

	private ILaunchConfiguration[] findExistingLaunchConfigurations(Object candidate) {
		if (!(candidate instanceof IJavaElement) && candidate instanceof IAdaptable) {
			candidate = ((IAdaptable) candidate).getAdapter(IJavaElement.class);
		}
		if (candidate instanceof IJavaElement) {
			IJavaElement element = (IJavaElement) candidate;
			IJavaElement elementToLaunch = null;
			try {
				switch (element.getElementType()) {
				case IJavaElement.JAVA_PROJECT:
				case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				case IJavaElement.PACKAGE_FRAGMENT:
				case IJavaElement.TYPE:
				case IJavaElement.METHOD:
					elementToLaunch = element;
					break;
				case IJavaElement.CLASS_FILE:
					if (element instanceof IOrdinaryClassFile)
						elementToLaunch = ((IOrdinaryClassFile) element).getType();
					break;
				case IJavaElement.COMPILATION_UNIT:
					elementToLaunch = ((ICompilationUnit) element).findPrimaryType();
					break;
				}
				if (elementToLaunch == null) {
					return null;
				}
				ILaunchConfigurationWorkingCopy workingCopy = createLaunchConfiguration(elementToLaunch);
				List<ILaunchConfiguration> list = findExistingLaunchConfigurations(workingCopy);
				return list.toArray(new ILaunchConfiguration[list.size()]);
			} catch (CoreException e) {
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IResource getLaunchableResource(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			if (ss.size() == 1) {
				Object selected = ss.getFirstElement();
				if (!(selected instanceof IJavaElement) && selected instanceof IAdaptable) {
					selected = ((IAdaptable) selected).getAdapter(IJavaElement.class);
				}
				if (selected instanceof IJavaElement) {
					return ((IJavaElement) selected).getResource();
				}
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IResource getLaunchableResource(IEditorPart editor) {
		ITypeRoot element = JavaUI.getEditorInputTypeRoot(editor.getEditorInput());
		if (element != null) {
			try {
				return element.getCorrespondingResource();
			} catch (JavaModelException e) {
			}
		}
		return null;
	}
}
