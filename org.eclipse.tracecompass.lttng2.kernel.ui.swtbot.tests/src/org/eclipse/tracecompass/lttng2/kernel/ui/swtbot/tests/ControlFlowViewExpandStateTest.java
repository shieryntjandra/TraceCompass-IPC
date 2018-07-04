/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.lttng2.kernel.ui.swtbot.tests;

import org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.controlflow.ControlFlowView;

/**
 * Test the expand state of time graph entries for the {@link ControlFlowView}
 *
 * @author Jean-Christian Kouame
 *
 */
public class ControlFlowViewExpandStateTest extends TimegraphViewExpandStateTestBase {

    @Override
    protected String getViewId() {
        return ControlFlowView.ID;
    }

    @Override
    protected String getViewTitle() {
        return "Control Flow";
    }

    @Override
    protected String[] getItemLabel() {
        return new String[] {"systemd", "kthreadd"};
    }
}
