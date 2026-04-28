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

package org.pocketworkstation.pckeyboard.material.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import org.pocketworkstation.pckeyboard.R
import org.pocketworkstation.pckeyboard.databinding.*

/**
 * RecyclerView adapter for Azahar-style smooth settings UI.
 * Manages multiple settings sections and items with Material 3 components.
 */
class SettingsAdapter(
    private val sections: List<SettingsSection>,
    private val repository: SettingsRepository
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_SECTION_CARD = 1
    }

    private val flatItems = mutableListOf<Any>()

    init {
        updateFlatItems()
    }

    private fun updateFlatItems() {
        flatItems.clear()
        sections.forEach { section ->
            flatItems.add(section.title) // Add header
            flatItems.add(section)       // Add section card containing items
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (flatItems[position]) {
            is String -> TYPE_HEADER
            is SettingsSection -> TYPE_SECTION_CARD
            else -> throw IllegalArgumentException("Unknown item type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                ItemSettingsHeaderBinding.inflate(inflater, parent, false)
            )
            TYPE_SECTION_CARD -> SectionViewHolder(
                ItemSettingsCardBinding.inflate(inflater, parent, false)
            )
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = flatItems[position]
        when (holder) {
            is HeaderViewHolder -> holder.bind(item as String)
            is SectionViewHolder -> holder.bind(item as SettingsSection)
        }
    }

    override fun getItemCount(): Int = flatItems.size

    inner class HeaderViewHolder(private val binding: ItemSettingsHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String) {
            binding.settingsHeaderTitle.text = title
        }
    }

    inner class SectionViewHolder(private val binding: ItemSettingsCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(section: SettingsSection) {
            binding.settingsItemsContainer.removeAllViews()
            val inflater = LayoutInflater.from(binding.root.context)

            section.items.forEachIndexed { index, item ->
                val itemView = createItemView(inflater, binding.settingsItemsContainer, item)
                binding.settingsItemsContainer.addView(itemView)

                // Add divider between items
                if (index < section.items.size - 1) {
                    val divider = View(binding.root.context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            binding.root.context.resources.getDimensionPixelSize(R.dimen.divider_height)
                        ).apply {
                            leftMargin = 64
                            rightMargin = 16
                        }
                        
                        val typedValue = android.util.TypedValue()
                        binding.root.context.theme.resolveAttribute(com.google.android.material.R.attr.colorOutlineVariant, typedValue, true)
                        setBackgroundColor(typedValue.data)
                        alpha = 0.5f
                    }
                    binding.settingsItemsContainer.addView(divider)
                }
            }
        }

        private fun createItemView(
            inflater: LayoutInflater,
            parent: ViewGroup,
            item: SettingsItem
        ): View {
            return when (item) {
                is BooleanSettingsItem -> bindBooleanItem(inflater, parent, item)
                is SliderSettingsItem -> bindSliderItem(inflater, parent, item)
                is ListSettingsItem -> bindListItem(inflater, parent, item)
                else -> View(inflater.context)
            }
        }

        private fun bindBooleanItem(
            inflater: LayoutInflater,
            parent: ViewGroup,
            item: BooleanSettingsItem
        ): View {
            val binding = ItemSettingsBooleanBinding.inflate(inflater, parent, false)
            val isChecked = repository.getBoolean(item)
            
            binding.settingsTitle.text = item.title
            binding.settingsSummary.text = item.getSummaryForState(isChecked)
            binding.settingsSwitch.isChecked = isChecked

            binding.settingsSwitch.setOnCheckedChangeListener { _, checked ->
                repository.setBoolean(item, checked)
                if (item.hasDynamicSummary()) {
                    binding.settingsSummary.text = item.getSummaryForState(checked)
                }
                notifyItemChanged(adapterPosition) // Handle dependencies
            }

            binding.root.setOnClickListener {
                binding.settingsSwitch.toggle()
            }

            binding.root.isEnabled = repository.isDependencySatisfied(item)
            binding.root.alpha = if (binding.root.isEnabled) 1.0f else 0.5f

            return binding.root
        }

        private fun bindSliderItem(
            inflater: LayoutInflater,
            parent: ViewGroup,
            item: SliderSettingsItem
        ): View {
            val binding = ItemSettingsSliderBinding.inflate(inflater, parent, false)
            val currentValue = repository.getSliderValue(item)

            binding.settingsTitle.text = item.title
            binding.settingsSummary.text = item.summary
            binding.settingsValue.text = item.formatValue(currentValue)
            
            binding.settingsSlider.valueFrom = item.minValue
            binding.settingsSlider.valueTo = item.maxValue
            binding.settingsSlider.stepSize = item.stepSize
            binding.settingsSlider.value = currentValue

            binding.settingsSlider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    repository.setSliderValue(item, value)
                    binding.settingsValue.text = item.formatValue(value)
                }
            }

            binding.root.isEnabled = repository.isDependencySatisfied(item)
            binding.root.alpha = if (binding.root.isEnabled) 1.0f else 0.5f
            binding.settingsSlider.isEnabled = binding.root.isEnabled

            return binding.root
        }

        private fun bindListItem(
            inflater: LayoutInflater,
            parent: ViewGroup,
            item: ListSettingsItem
        ): View {
            val binding = ItemSettingsListBinding.inflate(inflater, parent, false)
            val currentValue = repository.getListValue(item)

            binding.settingsTitle.text = item.title
            binding.settingsSummary.text = item.summary
            
            val adapter = ArrayAdapter(
                inflater.context,
                R.layout.item_dropdown,
                item.entries
            )
            binding.settingsDropdown.setAdapter(adapter)
            binding.settingsDropdown.setText(item.getEntryForValue(currentValue), false)

            binding.settingsDropdown.setOnItemClickListener { _, _, position, _ ->
                val newValue = item.entryValues[position]
                repository.setListValue(item, newValue)
                notifyItemChanged(adapterPosition) // Handle dependencies
            }

            binding.root.isEnabled = repository.isDependencySatisfied(item)
            binding.root.alpha = if (binding.root.isEnabled) 1.0f else 0.5f
            binding.settingsDropdown.isEnabled = binding.root.isEnabled

            return binding.root
        }
    }
}
