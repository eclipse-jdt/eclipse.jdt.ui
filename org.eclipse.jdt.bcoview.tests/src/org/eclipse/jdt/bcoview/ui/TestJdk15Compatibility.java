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
package org.eclipse.jdt.bcoview.ui;

public class TestJdk15Compatibility extends TestJdtUtils {

    public void testGetNamed1() throws Exception {
        doTest("Anon1");
    }

    public void testGetNamed1_1() throws Exception {
        doTest("Anon1_1");
    }

    public void testGetNamed2() throws Exception {
        doTest("Anon2");
    }

    public void testGetNamed3() throws Exception {
        doTest("Anon3");
    }

    public void testGetNamed3_3() throws Exception {
        doTest("Anon3_3");
    }

    public void testGetNamed4() throws Exception {
        doTest("Anon4");
    }

    public void testGetNamed5() throws Exception {
        doTest("Anon5");
    }

    @Override
    protected String getJdkVersion() {
        return "1.5";
    }

    @Override
    protected String getFieldName() {
        return "v15";
    }

}
