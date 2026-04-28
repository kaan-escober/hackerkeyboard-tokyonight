/*
 * Copyright (C) 2025 Hacker's Keyboard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pocketworkstation.pckeyboard.material

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.pocketworkstation.pckeyboard.R
import org.pocketworkstation.pckeyboard.material.settings.*

/**
 * Fragment for configuring keyboard input behavior settings.
 */
class InputBehaviorFragment : Fragment() {

    private lateinit var repository: SettingsRepository
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SettingsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Reuse fragment_theme_selection layout as it just contains a RecyclerView
        val view = inflater.inflate(R.layout.fragment_theme_selection, container, false)
        recyclerView = view.findViewById(R.id.theme_recycler_view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = SettingsRepository(requireContext())
        
        val sections = SettingsDefinitions.getInputBehaviorSections()
        adapter = SettingsAdapter(sections, repository)
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }
}
