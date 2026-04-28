package org.pocketworkstation.pckeyboard.material

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.pocketworkstation.pckeyboard.R
import org.pocketworkstation.pckeyboard.databinding.ItemSettingsHubCategoryBinding
import org.pocketworkstation.pckeyboard.databinding.ItemThemeBinding

class SettingsHubAdapter(
    private val items: List<HubItem>,
    private val onCategoryClick: (SettingsCategory) -> Unit,
    private val onThemeSelected: (ThemeSelectionFragment.ThemeItem) -> Unit,
    private val currentThemeId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_THEME_SCROLLER = 0
        const val TYPE_CATEGORY = 1
        const val TYPE_HEADER = 2
    }

    sealed class HubItem {
        data class ThemeScroller(val themes: List<ThemeSelectionFragment.ThemeItem>) : HubItem()
        data class Category(val category: SettingsCategory) : HubItem()
        data class Header(val title: String) : HubItem()
    }

    data class SettingsCategory(
        val id: Int,
        val title: String,
        val description: String,
        val iconRes: Int
    )

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is HubItem.ThemeScroller -> TYPE_THEME_SCROLLER
        is HubItem.Category -> TYPE_CATEGORY
        is HubItem.Header -> TYPE_HEADER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_THEME_SCROLLER -> ThemeScrollerViewHolder(
                inflater.inflate(R.layout.item_settings_theme_scroller, parent, false)
            )
            TYPE_CATEGORY -> CategoryViewHolder(
                ItemSettingsHubCategoryBinding.inflate(inflater, parent, false)
            )
            TYPE_HEADER -> HeaderViewHolder(
                inflater.inflate(R.layout.item_settings_header, parent, false)
            )
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is HubItem.ThemeScroller -> (holder as ThemeScrollerViewHolder).bind(item.themes)
            is HubItem.Category -> (holder as CategoryViewHolder).bind(item.category)
            is HubItem.Header -> (holder as HeaderViewHolder).bind(item.title)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class ThemeScrollerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val recyclerView: RecyclerView = view.findViewById(R.id.theme_scroller)
        fun bind(themes: List<ThemeSelectionFragment.ThemeItem>) {
            recyclerView.layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
            recyclerView.adapter = ThemeCompactAdapter(themes, onThemeSelected, currentThemeId)
        }
    }

    inner class CategoryViewHolder(private val binding: ItemSettingsHubCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(category: SettingsCategory) {
            binding.categoryTitle.text = category.title
            binding.categoryDescription.text = category.description
            binding.categoryIcon.setImageResource(category.iconRes)
            binding.categoryCard.setOnClickListener { onCategoryClick(category) }
        }
    }

    private class ThemeCompactAdapter(
        private val themes: List<ThemeSelectionFragment.ThemeItem>,
        private val onThemeSelected: (ThemeSelectionFragment.ThemeItem) -> Unit,
        private val currentThemeId: String
    ) : RecyclerView.Adapter<ThemeCompactAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_theme_compact, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val theme = themes[position]
            holder.name.text = theme.name
            
            val isSelected = theme.id == currentThemeId
            holder.radio.isChecked = isSelected
            
            val accentColor = getThemeAccentColor(holder.itemView.context, theme.id)
            if (isSelected) {
                holder.radio.buttonTintList = android.content.res.ColorStateList.valueOf(accentColor)
                (holder.itemView as com.google.android.material.card.MaterialCardView).strokeWidth = 4
                (holder.itemView as com.google.android.material.card.MaterialCardView).setStrokeColor(accentColor)
                (holder.itemView as com.google.android.material.card.MaterialCardView).setCardBackgroundColor(
                    android.content.res.ColorStateList.valueOf(accentColor).withAlpha(20)
                )
            } else {
                holder.radio.buttonTintList = null
                (holder.itemView as com.google.android.material.card.MaterialCardView).strokeWidth = 2
                val outlineValue = android.util.TypedValue()
                holder.itemView.context.theme.resolveAttribute(com.google.android.material.R.attr.colorOutlineVariant, outlineValue, true)
                (holder.itemView as com.google.android.material.card.MaterialCardView).setStrokeColor(outlineValue.data)
                (holder.itemView as com.google.android.material.card.MaterialCardView).setCardBackgroundColor(
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
                )
            }

            holder.colorBase.backgroundTintList = android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(holder.itemView.context, theme.baseColorRes))
            holder.colorAccent.backgroundTintList = android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(holder.itemView.context, theme.accentColorRes))

            holder.itemView.setOnClickListener { onThemeSelected(theme) }
        }

        override fun getItemCount(): Int = themes.size

        private fun getThemeAccentColor(context: android.content.Context, themeId: String): Int {
            val colorRes = when (themeId) {
                "10" -> R.color.tn_storm_green
                "11" -> R.color.tn_night_blue
                "13" -> R.color.tn_moon_purple
                "12" -> R.color.tn_day_blue
                else -> R.color.tn_storm_green
            }
            return androidx.core.content.ContextCompat.getColor(context, colorRes)
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.theme_name)
            val radio: android.widget.RadioButton = view.findViewById(R.id.theme_radio)
            val colorBase: View = view.findViewById(R.id.color_base)
            val colorAccent: View = view.findViewById(R.id.color_accent)
        }
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(title: String) {
            itemView.findViewById<TextView>(R.id.settings_header_title).text = title
        }
    }
}
