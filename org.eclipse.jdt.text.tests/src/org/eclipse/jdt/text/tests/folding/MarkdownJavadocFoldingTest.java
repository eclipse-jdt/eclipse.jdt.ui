package org.eclipse.jdt.text.tests.folding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.IRegion;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.Java22ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.Java23ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;

@RunWith(Parameterized.class)
public class MarkdownJavadocFoldingTest {
	@Rule
	public ProjectTestSetup projectSetup;

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	private IPackageFragment fPackageFragment;

	private final boolean extendedFoldingActive;

	private final boolean hasMarkdownJavadocs;

	public MarkdownJavadocFoldingTest(boolean extendedFoldingActive, boolean hasMarkdownJavadocs) {
		this.extendedFoldingActive= extendedFoldingActive;
		this.hasMarkdownJavadocs= hasMarkdownJavadocs;
		if (hasMarkdownJavadocs) {
			projectSetup= new Java23ProjectTestSetup(false);
		} else {
			projectSetup= new Java22ProjectTestSetup();
		}
	}

	@Parameters(name= "Extended folding active: {0}, JDK compilance with markdown Javadocs: {1}")
	public static List<Object[]> parameters() {
		return List.of(
				new Object[] { true, true },
				new Object[] { true, false },
				new Object[] { false, true },
				new Object[] { false, false });
	}

	@Before
	public void setUp() throws CoreException {
		fJProject1= projectSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		fPackageFragment= fSourceFolder.createPackageFragment("org.example.test", false, null);
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.EDITOR_NEW_FOLDING_ENABLED, extendedFoldingActive);
	}

	@After
	public void tearDown() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setToDefault(PreferenceConstants.EDITOR_NEW_FOLDING_ENABLED);
	}

	@Test
	public void testClassWithMarkdownJavadocAsHeaderComment() throws Exception {
		String str= """
				package org.example.test;
				/// Javadoc							//here should be an annotation
				/// comment
				/// here
				public class HeaderCommentTest {
				}
				""";
		if (hasMarkdownJavadocs) {
			FoldingTestUtils.assertCodeHasRegions(fPackageFragment, "TestFolding.java", str, 1);
			List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(fPackageFragment, "TestFolding.java", str);
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 1, 3); // Javadoc
		} else {
			FoldingTestUtils.assertCodeHasRegions(fPackageFragment, "TestFolding.java", str, 0);
		}
	}

	@Test
	public void testSingleMethodWithMarkdownJavadoc() throws Exception {
		String str= """
				package org.example.test;
				public class SingleMethodTest {
				    /// Javadoc							//here should be an annotation
				    /// comment
				    /// here
				    public void foo() {					//here should be an annotation
				        System.out.println("Hello");
				    }
				}
				""";
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(fPackageFragment, "TestFolding.java", str);
		if (hasMarkdownJavadocs) {
			assertEquals(2, regions.size());
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 4); // Javadoc
		} else {
			assertEquals(1, regions.size());
		}
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 5, 6); // foo method
	}
}
