package org.eclipse.jdt.ui.tests.dialogs;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.TestPluginLauncher;


/**
 * Test all areas of the UI.
 */
public class UIInteractiveSuite extends TestSuite {

	/**
	 * Construct the test suite.
	 */
	public UIInteractiveSuite() {
		//addTest(new TestSuite(PreferencesTest.class));
		//addTest(new TestSuite(WizardsTest.class));
		addTest(new TestSuite(DialogsTest.class));
		
	}
	
	public static void main(String[] args) {
		TestPluginLauncher.run(TestPluginLauncher.getLocationFromProperties(), UIInteractiveSuite.class, args);
	}		


}