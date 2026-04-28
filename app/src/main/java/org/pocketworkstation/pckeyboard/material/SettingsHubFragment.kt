package org.pocketworkstation.pckeyboard.material

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import org.pocketworkstation.pckeyboard.KeyboardSwitcher
import org.pocketworkstation.pckeyboard.R
import org.pocketworkstation.pckeyboard.databinding.FragmentSettingsHubBinding

class SettingsHubFragment : Fragment() {

    private var _binding: FragmentSettingsHubBinding? = null
    private val binding get() = _binding!!
    private var loadingDialog: Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsHubBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val currentThemeId = prefs.getString(KeyboardSwitcher.PREF_KEYBOARD_LAYOUT, "10") ?: "10"

        val themes = listOf(
            ThemeSelectionFragment.ThemeItem("10", "Tokyo Night Storm", "Deep blue variant",
                R.color.tn_storm_bg, R.color.tn_storm_bg_dark, R.color.tn_storm_terminal_black,
                R.color.tn_storm_blue, R.color.tn_storm_fg, R.color.tn_storm_green),
            ThemeSelectionFragment.ThemeItem("11", "Tokyo Night Night", "Pure dark variant",
                R.color.tn_night_bg, R.color.tn_night_bg_dark, R.color.tn_night_terminal_black,
                R.color.tn_night_cyan, R.color.tn_night_fg, R.color.tn_night_magenta),
            ThemeSelectionFragment.ThemeItem("13", "Tokyo Night Moon", "Moonlit blue variant",
                R.color.tn_moon_bg, R.color.tn_moon_bg_dark, R.color.tn_moon_terminal_black,
                R.color.tn_moon_purple, R.color.tn_moon_fg, R.color.tn_moon_green),
            ThemeSelectionFragment.ThemeItem("12", "Tokyo Night Day", "Light mode variant",
                R.color.tn_day_bg, R.color.tn_day_bg_dark, R.color.tn_day_terminal_black,
                R.color.tn_day_blue, R.color.tn_day_fg, R.color.tn_day_green)
        )

        val categories = listOf(
            SettingsHubAdapter.SettingsCategory(1, "Visual Appearance", 
                "Keyboard height, popups, and display options", R.drawable.ic_visual),
            SettingsHubAdapter.SettingsCategory(2, "Input Behavior", 
                "Auto-capitalization and long-press duration", R.drawable.ic_input),
            SettingsHubAdapter.SettingsCategory(3, "Feedback", 
                "Vibration and sound effects while typing", R.drawable.ic_feedback),
            SettingsHubAdapter.SettingsCategory(4, "Advanced & Gestures", 
                "Key behavior, swipe actions, and debugging", R.drawable.ic_advanced)
        )

        val items = mutableListOf<SettingsHubAdapter.HubItem>()
        items.add(SettingsHubAdapter.HubItem.Header("Appearance"))
        items.add(SettingsHubAdapter.HubItem.ThemeScroller(themes))
        items.add(SettingsHubAdapter.HubItem.Header("Keyboard Settings"))
        categories.forEach { items.add(SettingsHubAdapter.HubItem.Category(it)) }

        binding.settingsHubRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.settingsHubRecyclerView.adapter = SettingsHubAdapter(
            items, 
            { onCategoryClicked(it) },
            { onThemeSelected(it) },
            currentThemeId
        )
    }

    private fun onThemeSelected(theme: ThemeSelectionFragment.ThemeItem) {
        showLoadingDialog()
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        prefs.edit().putString(KeyboardSwitcher.PREF_KEYBOARD_LAYOUT, theme.id).apply()
        
        Handler(Looper.getMainLooper()).postDelayed({
            activity?.recreate()
        }, 300)
    }

    private fun showLoadingDialog() {
        loadingDialog = Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false)
            val progressIndicator = CircularProgressIndicator(requireContext()).apply {
                isIndeterminate = true
                val typedValue = TypedValue()
                requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
                setIndicatorColor(typedValue.data)
                trackThickness = 8
                indicatorSize = 64
            }
            val cardView = MaterialCardView(requireContext()).apply {
                val typedValue = TypedValue()
                requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainerHigh, typedValue, true)
                setCardBackgroundColor(typedValue.data)
                cardElevation = 16f
                radius = 24f
                setContentPadding(48, 48, 48, 48)
                addView(progressIndicator)
            }
            setContentView(cardView)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            show()
        }
    }

    private fun onCategoryClicked(category: SettingsHubAdapter.SettingsCategory) {
        val fragment = when (category.id) {
            1 -> VisualAppearanceFragment()
            2 -> InputBehaviorFragment()
            3 -> FeedbackFragment()
            4 -> GesturesFragment()
            else -> null
        }

        fragment?.let {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.anim_settings_fragment_in,
                    R.anim.anim_settings_fragment_out,
                    R.anim.anim_settings_fragment_in,
                    R.anim.anim_settings_fragment_out
                )
                .replace(R.id.settings_tab_container, it)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadingDialog?.dismiss()
        _binding = null
    }
}
