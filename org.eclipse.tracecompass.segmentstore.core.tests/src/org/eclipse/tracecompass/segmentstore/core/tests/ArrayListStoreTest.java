/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.segmentstore.core.tests;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.segmentstore.core.arraylist.ArrayListStore;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;

/**
 * Unit tests for intersecting elements in an ArrayListStore
 *
 * @author France Lapointe Nguyen
 * @author Matthew Khouzam
 */
public class ArrayListStoreTest extends AbstractTestSegmentStore {

    @Override
    protected ISegmentStore<@NonNull TestSegment> getSegmentStore() {
        return new ArrayListStore<>();
    }

    @Override
    protected ISegmentStore<@NonNull TestSegment> getSegmentStore(@NonNull TestSegment @NonNull [] data) {
        return new ArrayListStore<>(data);
    }
}