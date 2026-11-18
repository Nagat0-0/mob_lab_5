package com.example.cs2skinswipe.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.cs2skinswipe.R
import com.example.cs2skinswipe.data.SkinEntity
import java.util.Collections

class SavedSkinsAdapter(
    private val skins: MutableList<SkinEntity>,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit,
    private val onClick: (SkinEntity) -> Unit
) : RecyclerView.Adapter<SavedSkinsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tvRank)
        val ivSkin: ImageView = view.findViewById(R.id.ivSavedSkin)
        val tvName: TextView = view.findViewById(R.id.tvSavedSkinName)
        val viewRarityLine: View = view.findViewById(R.id.viewRarityLine)
        val tvDragHandle: TextView = view.findViewById(R.id.tvDragHandle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved_skin, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val skin = skins[position]
        holder.tvRank.text = "#${position + 1}"
        holder.tvName.text = skin.name
        holder.ivSkin.load(skin.imageUrl) {
            crossfade(true)
        }
        try {
            holder.viewRarityLine.setBackgroundColor(Color.parseColor(skin.rarityColor))
        } catch (e: Exception) {
            holder.viewRarityLine.setBackgroundColor(Color.parseColor("#A78BFA"))
        }
        holder.itemView.setOnClickListener { onClick(skin) }
        holder.tvDragHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                onStartDrag(holder)
            }
            false
        }
    }

    override fun getItemCount(): Int = skins.size

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(skins, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(skins, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    fun getUpdatedSkins(): List<SkinEntity> {
        skins.forEachIndexed { index, skin ->
            skin.sortIndex = index
        }
        notifyDataSetChanged()
        return skins.toList()
    }
}