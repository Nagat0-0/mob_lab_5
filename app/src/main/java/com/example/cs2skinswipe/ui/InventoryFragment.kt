package com.example.cs2skinswipe.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.cs2skinswipe.R
import com.example.cs2skinswipe.data.AppDatabase
import com.example.cs2skinswipe.data.SkinEntity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class InventoryFragment : Fragment(R.layout.fragment_inventory) {

    private lateinit var btnBack: ImageButton
    private lateinit var tvHeaderTitle: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var recyclerView: RecyclerView

    private var currentLevel = 0
    private var selectedCategory: String? = null
    private var selectedWeapon: String? = null
    private var currentJob: Job? = null
    private var itemTouchHelper: ItemTouchHelper? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnBack = view.findViewById(R.id.btnBack)
        tvHeaderTitle = view.findViewById(R.id.tvHeaderTitle)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        btnBack.setOnClickListener {
            when (currentLevel) {
                2 -> {
                    if (selectedWeapon != null) {
                        selectedWeapon = null
                        showWeapons(selectedCategory ?: "")
                    } else {
                        selectedCategory = null
                        showCategories()
                    }
                }
                1 -> {
                    selectedCategory = null
                    showCategories()
                }
            }
        }

        showCategories()
    }

    private fun showCategories() {
        currentLevel = 0
        btnBack.visibility = View.GONE
        tvHeaderTitle.text = "Loadout"
        detachDragHelper()

        val dao = AppDatabase.getDatabase(requireContext()).skinDao()
        currentJob?.cancel()
        currentJob = viewLifecycleOwner.lifecycleScope.launch {
            dao.getSavedCategories().collect { categories ->
                if (categories.isEmpty()) {
                    tvEmptyState.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    tvEmptyState.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    recyclerView.adapter = SimpleListAdapter(categories) { category ->
                        selectedCategory = category
                        if (category.equals("Knives", ignoreCase = true) ||
                            category.equals("Gloves", ignoreCase = true)
                        ) {
                            selectedWeapon = null
                            showSkinsByCategory(category)
                        } else {
                            showWeapons(category)
                        }
                    }
                }
            }
        }
    }

    private fun showWeapons(category: String) {
        currentLevel = 1
        btnBack.visibility = View.VISIBLE
        tvHeaderTitle.text = category
        detachDragHelper()

        val dao = AppDatabase.getDatabase(requireContext()).skinDao()
        currentJob?.cancel()
        currentJob = viewLifecycleOwner.lifecycleScope.launch {
            dao.getWeaponsByCategory(category).collect { weapons ->
                if (weapons.isEmpty()) {
                    showCategories()
                } else {
                    tvEmptyState.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    recyclerView.adapter = SimpleListAdapter(weapons) { weapon ->
                        selectedWeapon = weapon
                        showSkinsByWeapon(weapon)
                    }
                }
            }
        }
    }

    private fun showSkinsByCategory(category: String) {
        currentLevel = 2
        btnBack.visibility = View.VISIBLE
        tvHeaderTitle.text = category

        val dao = AppDatabase.getDatabase(requireContext()).skinDao()
        currentJob?.cancel()
        currentJob = viewLifecycleOwner.lifecycleScope.launch {
            dao.getSkinsByCategory(category).collect { skins ->
                if (skins.isEmpty()) {
                    showCategories()
                } else {
                    bindSkinsAdapter(skins)
                }
            }
        }
    }

    private fun showSkinsByWeapon(weapon: String) {
        currentLevel = 2
        btnBack.visibility = View.VISIBLE
        tvHeaderTitle.text = weapon

        val dao = AppDatabase.getDatabase(requireContext()).skinDao()
        currentJob?.cancel()
        currentJob = viewLifecycleOwner.lifecycleScope.launch {
            dao.getSkinsByWeapon(weapon).collect { skins ->
                if (skins.isEmpty()) {
                    showWeapons(selectedCategory ?: "")
                } else {
                    bindSkinsAdapter(skins)
                }
            }
        }
    }

    private fun bindSkinsAdapter(skins: List<SkinEntity>) {
        tvEmptyState.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        val adapter = SavedSkinsAdapter(
            skins = skins.toMutableList(),
            onStartDrag = { viewHolder -> itemTouchHelper?.startDrag(viewHolder) },
            onClick = { clickedSkin -> showSkinDetailBottomSheet(clickedSkin) }
        )
        recyclerView.adapter = adapter
        attachDragHelper(adapter)
    }

    private fun attachDragHelper(adapter: SavedSkinsAdapter) {
        detachDragHelper()
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                rv: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                adapter.moveItem(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun clearView(rv: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(rv, viewHolder)
                val updated = adapter.getUpdatedSkins()
                val dao = AppDatabase.getDatabase(requireContext()).skinDao()
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    dao.updateSkins(updated)
                }
            }
        }
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper?.attachToRecyclerView(recyclerView)
    }

    private fun detachDragHelper() {
        itemTouchHelper?.attachToRecyclerView(null)
        itemTouchHelper = null
    }

    private fun showSkinDetailBottomSheet(skin: SkinEntity) {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = LayoutInflater.from(requireContext())
            .inflate(R.layout.bottom_sheet_skin_detail, null)

        val ivImage = sheetView.findViewById<ImageView>(R.id.ivDetailImage)
        val tvWeapon = sheetView.findViewById<TextView>(R.id.tvDetailWeapon)
        val tvName = sheetView.findViewById<TextView>(R.id.tvDetailName)
        val tvDesc = sheetView.findViewById<TextView>(R.id.tvDetailDescription)
        val btnRemove = sheetView.findViewById<MaterialButton>(R.id.btnRemoveSkin)

        ivImage.load(skin.imageUrl) { crossfade(true) }
        tvWeapon.text = "${skin.category} • ${skin.weaponName}"
        tvName.text = skin.name
        tvDesc.text = skin.description

        btnRemove.setOnClickListener {
            val dao = AppDatabase.getDatabase(requireContext()).skinDao()
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                dao.deleteSkin(skin.id)
            }
            dialog.dismiss()
        }

        dialog.setContentView(sheetView)
        dialog.show()
    }
}