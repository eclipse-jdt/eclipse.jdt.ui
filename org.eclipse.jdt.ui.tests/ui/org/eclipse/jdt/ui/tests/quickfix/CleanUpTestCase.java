package org.eclipse.jdt.ui.tests.quickfix;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.CreateChangeOperation;
import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CleanUpPreferenceUtil;
import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.fix.ICleanUp;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpProfileVersioner;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileStore;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.CustomProfile;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

public class CleanUpTestCase extends QuickFixTest {

	protected static final String FIELD_COMMENT= "/* Test */";
	
	protected IPackageFragmentRoot fSourceFolder;
	protected IJavaProject fJProject1;
	
	private CustomProfile fProfile;

	public static Test allTests() {
		TestSuite suite= new TestSuite();

		suite.addTest(CleanUpStressTest.suite());
		suite.addTest(CleanUpTest.suite());
		suite.addTest(CleanUpAnnotationTest.suite());

		return suite;
	}

	public static Test suite() {
		if (true)
			return allTests();

		return setUpTest(new CleanUpTest("testRemoveBlock05"));
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}

	public CleanUpTestCase(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		Hashtable options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");

		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);
		store.setValue(PreferenceConstants.CODEGEN_KEYWORD_THIS, false);

		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "//TODO\n${body_statement}", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.FIELDCOMMENT_ID, FIELD_COMMENT, null);

		Preferences corePrefs= JavaCore.getPlugin().getPluginPreferences();
		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_PREFIXES, "");
		corePrefs.setValue(JavaCore.CODEASSIST_STATIC_FIELD_PREFIXES, "");
		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_SUFFIXES, "");
		corePrefs.setValue(JavaCore.CODEASSIST_STATIC_FIELD_SUFFIXES, "");

		fJProject1= ProjectTestSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		Map settings= new Hashtable();
		fProfile= new ProfileManager.CustomProfile("testProfile", settings, CleanUpProfileVersioner.CURRENT_VERSION, CleanUpProfileVersioner.PROFILE_KIND);
		new InstanceScope().getNode(JavaUI.ID_PLUGIN).put(CleanUpConstants.CLEANUP_PROFILE, fProfile.getID());

		disableAll();
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
		disableAll();
	}

	private void disableAll() throws CoreException {
		Map settings= fProfile.getSettings();
		Collection keys= CleanUpConstants.getEclipseDefaultSettings().keySet();
		for (Iterator iterator= keys.iterator(); iterator.hasNext();) {
			String key= (String)iterator.next();
			settings.put(key, CleanUpConstants.FALSE);
		}
		commitProfile();
	}

	protected void enable(String key) throws CoreException {
		fProfile.getSettings().put(key, CleanUpConstants.TRUE);
		commitProfile();
	}

	private void commitProfile() throws CoreException {
		List profiles= CleanUpPreferenceUtil.getBuiltInProfiles();
		profiles.add(fProfile);

		CleanUpProfileVersioner versioner= new CleanUpProfileVersioner();
		ProfileStore profileStore= new ProfileStore(CleanUpConstants.CLEANUP_PROFILES, versioner);
		profileStore.writeProfiles(profiles, new InstanceScope());
	}

	protected RefactoringStatus assertRefactoringResultAsExpected(ICompilationUnit[] cus, String[] expected) throws CoreException {
		RefactoringStatus status= performRefactoring(cus);

		String[] previews= new String[cus.length];
		for (int i= 0; i < cus.length; i++) {
			ICompilationUnit cu= cus[i];
			previews[i]= cu.getBuffer().getContents();
		}

		assertEqualStringsIgnoreOrder(previews, expected);
		
		return status;
	}

	protected void assertRefactoringResultAsExpectedIgnoreHashValue(ICompilationUnit[] cus, String[] expected) throws CoreException {
		performRefactoring(cus);

		Pattern regex= Pattern.compile("long serialVersionUID = .*L;");

		String[] previews= new String[cus.length];
		for (int i= 0; i < cus.length; i++) {
			ICompilationUnit cu= cus[i];
			previews[i]= cu.getBuffer().getContents().replaceAll(regex.pattern(), "long serialVersionUID = 1L;");
		}

		assertEqualStringsIgnoreOrder(previews, expected);
	}

	protected RefactoringStatus assertRefactoringHasNoChange(ICompilationUnit[] cus) throws CoreException, InvocationTargetException {
		String[] expected= new String[cus.length];
		for (int i= 0; i < cus.length; i++) {
			expected[i]= cus[i].getBuffer().getContents();
		}
		return assertRefactoringResultAsExpected(cus, expected);
	}
	
	protected final RefactoringStatus performRefactoring(ICompilationUnit[] cus) throws CoreException {
		final CleanUpRefactoring ref= new CleanUpRefactoring();
		for (int i= 0; i < cus.length; i++) {
			ref.addCompilationUnit(cus[i]);
		}
		
		ICleanUp[] cleanUps= CleanUpRefactoring.createCleanUps();
		for (int i= 0; i < cleanUps.length; i++) {
			ref.addCleanUp(cleanUps[i]);
		}

		IUndoManager undoManager= getUndoManager();
		final CreateChangeOperation create= new CreateChangeOperation(
			new CheckConditionsOperation(ref, CheckConditionsOperation.ALL_CONDITIONS),
			RefactoringStatus.FATAL);
		
		final PerformChangeOperation perform= new PerformChangeOperation(create);
		perform.setUndoManager(undoManager, ref.getName());
		
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		executePerformOperation(perform, workspace);
			
		RefactoringStatus status= create.getConditionCheckingStatus();
		if (status.hasFatalError()) {
			throw new CoreException(new StatusInfo(status.getSeverity(), status.getMessageMatchingSeverity(status.getSeverity())));
		}
		
		assertTrue("Change wasn't executed", perform.changeExecuted());
		
		Change undo= perform.getUndoChange();
		assertNotNull("Undo doesn't exist", undo);
		assertTrue("Undo manager is empty", undoManager.anythingToUndo());
		
		return status;
	}
	
	private IUndoManager getUndoManager() {
		IUndoManager undoManager= RefactoringCore.getUndoManager();
		undoManager.flush();
		return undoManager;
	}
	
	private void executePerformOperation(final PerformChangeOperation perform, IWorkspace workspace) throws CoreException {
		workspace.run(perform, new NullProgressMonitor());
	}

}
