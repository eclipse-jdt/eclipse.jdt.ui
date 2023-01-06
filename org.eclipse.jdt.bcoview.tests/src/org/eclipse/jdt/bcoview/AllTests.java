/*******************************************************************************
 * Copyright (c) 2018 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.bcoview;

import org.eclipse.jdt.bcoview.ui.TestJdk14Compatibility;
import org.eclipse.jdt.bcoview.ui.TestJdk15Compatibility;

import junit.framework.Test;
import junit.framework.TestSuite;


public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for org.eclipse.jdt.bcoview");
        //$JUnit-BEGIN$
        suite.addTestSuite(TestJdk14Compatibility.class);
        suite.addTestSuite(TestJdk15Compatibility.class);
        //$JUnit-END$
        return suite;
    }

}
