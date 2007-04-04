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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ui.PlatformUI;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CleanUpPreferenceUtil;
import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringExecutionStarter;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.fix.ICleanUp;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpProfileVersioner;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileStore;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.CustomProfile;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringExecutionHelper;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringSaveHelper;

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

	protected RefactoringStatus assertRefactoringResultAsExpected(ICompilationUnit[] cus, String[] expected) throws InvocationTargetException, JavaModelException {
		Shell shell= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		
		final RefactoringStatus[] conditionCheck= new RefactoringStatus[1];
		final CleanUpRefactoring refactoring= new CleanUpRefactoring() {
			public RefactoringStatus checkAllConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
			    RefactoringStatus conditions= super.checkAllConditions(pm);
			    conditionCheck[0]= conditions;
				return conditions;
			}
		};
		for (int i= 0; i < cus.length; i++) {
			refactoring.addCompilationUnit(cus[i]);
		}
		
		ICleanUp[] cleanUps= CleanUpRefactoring.createCleanUps();
		for (int i= 0; i < cleanUps.length; i++) {
			refactoring.addCleanUp(cleanUps[i]);
		}

		RefactoringExecutionHelper helper= new RefactoringExecutionHelper(refactoring, IStatus.ERROR, RefactoringSaveHelper.SAVE_JAVA_ONLY_UPDATES, shell, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		try {
			helper.perform(true, true);
		} catch (InterruptedException e) {
		}

		String[] previews= new String[cus.length];
		for (int i= 0; i < cus.length; i++) {
			ICompilationUnit cu= cus[i];
			previews[i]= cu.getBuffer().getContents();
		}

		assertEqualStringsIgnoreOrder(previews, expected);
		
		return conditionCheck[0];
	}

	protected void assertRefactoringResultAsExpectedIgnoreHashValue(ICompilationUnit[] cus, String[] expected) throws InvocationTargetException, JavaModelException {
		RefactoringExecutionStarter.startCleanupRefactoring(cus, CleanUpRefactoring.createCleanUps(), PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), false, "Clean Up");

		Pattern regex= Pattern.compile("long serialVersionUID = .*L;");

		String[] previews= new String[cus.length];
		for (int i= 0; i < cus.length; i++) {
			ICompilationUnit cu= cus[i];
			previews[i]= cu.getBuffer().getContents().replaceAll(regex.pattern(), "long serialVersionUID = 1L;");
		}

		assertEqualStringsIgnoreOrder(previews, expected);
	}

	protected void assertRefactoringHasNoChange(ICompilationUnit[] cus) throws JavaModelException, InvocationTargetException {
		String[] expected= new String[cus.length];
		for (int i= 0; i < cus.length; i++) {
			expected[i]= cus[i].getBuffer().getContents();
		}
		assertRefactoringResultAsExpected(cus, expected);
	}

}
