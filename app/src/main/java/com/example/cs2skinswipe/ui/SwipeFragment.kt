package com.example.cs2skinswipe.ui

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.cs2skinswipe.R
import com.example.cs2skinswipe.data.AppDatabase
import com.example.cs2skinswipe.data.SkinEntity
import com.example.cs2skinswipe.network.ApiSkin
import com.example.cs2skinswipe.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.min

class SwipeFragment : Fragment(R.layout.fragment_swipe) {

    private var allUnseenSkins: MutableList<ApiSkin> = mutableListOf()
    private var filteredSkins: MutableList<ApiSkin> = mutableListOf()
    private var selectedCategory: String = "All"
    private var currentRarityColor: Int = Color.parseColor("#A78BFA")
    private var dX = 0f

    private val history = ArrayDeque<Pair<ApiSkin, Boolean>>()

    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var progressBar: ProgressBar
    private lateinit var tvNoMoreSkins: TextView
    private lateinit var skinCard: MaterialCardView
    private lateinit var skinImage: ImageView
    private lateinit var weaponNameText: TextView
    private lateinit var skinNameText: TextView
    private lateinit var categoryBadge: TextView
    private lateinit var rarityBar: View
    private lateinit var tvSwipeSave: TextView
    private lateinit var tvSwipeSkip: TextView
    private lateinit var btnSkip: MaterialButton
    private lateinit var btnUndo: MaterialButton
    private lateinit var btnSave: MaterialButton

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chipGroupCategories = view.findViewById(R.id.chipGroupCategories)
        progressBar = view.findViewById(R.id.progressBar)
        tvNoMoreSkins = view.findViewById(R.id.tvNoMoreSkins)
        skinCard = view.findViewById(R.id.skinCard)
        skinImage = view.findViewById(R.id.skinImage)
        weaponNameText = view.findViewById(R.id.weaponNameText)
        skinNameText = view.findViewById(R.id.skinNameText)
        categoryBadge = view.findViewById(R.id.categoryBadge)
        rarityBar = view.findViewById(R.id.rarityBar)
        tvSwipeSave = view.findViewById(R.id.tvSwipeSave)
        tvSwipeSkip = view.findViewById(R.id.tvSwipeSkip)
        btnSkip = view.findViewById(R.id.btnSkip)
        btnUndo = view.findViewById(R.id.btnUndo)
        btnSave = view.findViewById(R.id.btnSave)

        btnSkip.setOnClickListener { animateAndNext(false) }
        btnSave.setOnClickListener { animateAndNext(true) }
        btnUndo.setOnClickListener { undoLastSwipe() }

        skinCard.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = v.x - event.rawX
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val newX = event.rawX + dX
                    v.x = newX
                    val shift = v.translationX
                    v.rotation = shift / 25f
                    val progress = min(abs(shift) / 250f, 1f)
                    if (shift > 0) {
                        tvSwipeSave.alpha = progress
                        tvSwipeSkip.alpha = 0f
                        skinCard.strokeColor = Color.parseColor("#34D399")
                    } else if (shift < 0) {
                        tvSwipeSkip.alpha = progress
                        tvSwipeSave.alpha = 0f
                        skinCard.strokeColor = Color.parseColor("#F87171")
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val shift = v.translationX
                    if (abs(shift) > 250f) {
                        animateAndNext(shift > 0)
                    } else {
                        resetCardPosition()
                    }
                    true
                }
                else -> false
            }
        }

        loadSkins()
    }

    private fun resetCardPosition() {
        skinCard.animate().translationX(0f).rotation(0f).setDuration(200).start()
        tvSwipeSave.alpha = 0f
        tvSwipeSkip.alpha = 0f
        skinCard.strokeColor = currentRarityColor
    }

    private fun loadSkins() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val dao = AppDatabase.getDatabase(requireContext()).skinDao()
                val savedIds = withContext(Dispatchers.IO) {
                    dao.getAllSavedIds().toSet()
                }
                val fetched = withContext(Dispatchers.IO) {
                    RetrofitClient.api.getSkins()
                }
                allUnseenSkins = fetched.filter {
                    !it.image.isNullOrEmpty() &&
                            !it.weapon?.name.isNullOrEmpty() &&
                            !it.category?.name.isNullOrEmpty() &&
                            !savedIds.contains(it.id)
                }.shuffled().toMutableList()

                progressBar.visibility = View.GONE
                setupCategoryChips()
                applyCategoryFilter("All")
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "API Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupCategoryChips() {
        chipGroupCategories.removeAllViews()
        val categories = listOf("All") + allUnseenSkins
            .mapNotNull { it.category?.name }
            .distinct()
            .sorted()

        categories.forEach { category ->
            val chip = Chip(requireContext()).apply {
                text = category
                isCheckable = true
                isChecked = category == selectedCategory
                setTextColor(Color.WHITE)
                chipStrokeWidth = 2f
                chipStrokeColor = ColorStateList.valueOf(Color.parseColor("#2D3142"))
                chipBackgroundColor = ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf()
                    ),
                    intArrayOf(
                        Color.parseColor("#A78BFA"),
                        Color.parseColor("#1A1D2B")
                    )
                )
                setOnClickListener {
                    selectedCategory = category
                    applyCategoryFilter(category)
                }
            }
            chipGroupCategories.addView(chip)
        }
    }

    private fun applyCategoryFilter(category: String) {
        filteredSkins = if (category == "All") {
            allUnseenSkins.toMutableList()
        } else {
            allUnseenSkins.filter { it.category?.name == category }.toMutableList()
        }
        showCurrentSkin()
    }

    private fun showCurrentSkin() {
        btnUndo.isEnabled = history.isNotEmpty()
        btnUndo.alpha = if (history.isNotEmpty()) 1f else 0.4f

        if (filteredSkins.isEmpty()) {
            skinCard.visibility = View.GONE
            tvNoMoreSkins.visibility = View.VISIBLE
            return
        }

        tvNoMoreSkins.visibility = View.GONE
        skinCard.visibility = View.VISIBLE
        val skin = filteredSkins.first()

        skinCard.translationX = 0f
        skinCard.rotation = 0f
        skinCard.alpha = 1f
        tvSwipeSave.alpha = 0f
        tvSwipeSkip.alpha = 0f

        val parts = skin.name.split(" | ")
        val weapon = skin.weapon?.name ?: parts.firstOrNull() ?: "Unknown"
        val skinTitle = if (parts.size > 1) parts[1] else skin.name

        weaponNameText.text = weapon
        skinNameText.text = skinTitle
        categoryBadge.text = skin.category?.name?.uppercase() ?: "SKIN"
        skinImage.load(skin.image) {
            crossfade(true)
        }

        val colorHex = skin.rarity?.color ?: "#A78BFA"
        currentRarityColor = try {
            Color.parseColor(colorHex)
        } catch (e: Exception) {
            Color.parseColor("#A78BFA")
        }
        rarityBar.setBackgroundColor(currentRarityColor)
        skinCard.strokeColor = currentRarityColor
    }

    private fun animateAndNext(save: Boolean) {
        if (filteredSkins.isEmpty()) return
        val currentSkin = filteredSkins.removeAt(0)
        allUnseenSkins.remove(currentSkin)
        history.addLast(currentSkin to save)

        if (save) {
            tvSwipeSave.alpha = 1f
            skinCard.strokeColor = Color.parseColor("#34D399")
            saveSkinToDb(currentSkin)
        } else {
            tvSwipeSkip.alpha = 1f
            skinCard.strokeColor = Color.parseColor("#F87171")
        }

        val targetX = if (save) 1200f else -1200f
        skinCard.animate()
            .translationX(targetX)
            .rotation(if (save) 25f else -25f)
            .alpha(0f)
            .setDuration(250)
            .withEndAction {
                showCurrentSkin()
            }
            .start()
    }

    private fun undoLastSwipe() {
        if (history.isEmpty()) return
        val (lastSkin, wasSaved) = history.removeLast()

        if (wasSaved) {
            val dao = AppDatabase.getDatabase(requireContext()).skinDao()
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                dao.deleteSkin(lastSkin.id)
            }
        }

        allUnseenSkins.add(0, lastSkin)
        if (selectedCategory == "All" || lastSkin.category?.name == selectedCategory) {
            filteredSkins.add(0, lastSkin)
        }
        showCurrentSkin()
    }

    private fun saveSkinToDb(skin: ApiSkin) {
        val dao = AppDatabase.getDatabase(requireContext()).skinDao()
        val parts = skin.name.split(" | ")
        val weapon = skin.weapon?.name ?: parts.firstOrNull() ?: "Unknown"
        val skinTitle = if (parts.size > 1) parts[1] else skin.name
        val category = skin.category?.name ?: "Other"

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val count = dao.getSkinCountForWeapon(weapon)
            val entity = SkinEntity(
                id = skin.id,
                name = skinTitle,
                weaponName = weapon,
                category = category,
                rarityColor = skin.rarity?.color ?: "#A78BFA",
                imageUrl = skin.image ?: "",
                description = skin.description ?: "No description available.",
                sortIndex = count
            )
            dao.insertSkin(entity)
        }
    }
}