/*
 * Copyright (C) 2011 Darren Salt
 *
 * Licensed under the Apache License, Version 2.0 (the "Licence"); you may
 * not use this file except in compliance with the Licence. You may obtain
 * a copy of the Licence at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * Licence for the specific language governing permissions and limitations
 * under the Licence.
 */

package org.pocketworkstation.pckeyboard;

import android.view.inputmethod.EditorInfo;

/**
 * Interface for handling compose sequence callbacks.
 * 
 * Defines the contract for components that consume compose sequence results,
 * providing methods to output text, update keyboard state, and query the current
 * input editor context.
 */
public interface ComposeSequencing {
    /**
     * Called when a compose sequence completes and produces text output.
     * 
     * @param text The resulting character sequence from the completed compose sequence
     */
    public void onText(CharSequence text);
    
    /**
     * Updates the shift key state based on the current input context.
     * 
     * @param attr The current editor information context
     */
    public void updateShiftKeyState(EditorInfo attr);
    
    /**
     * Retrieves the current input editor context information.
     * 
     * @return The current EditorInfo, or null if no editor is active
     */
    public EditorInfo getCurrentInputEditorInfo();
}
