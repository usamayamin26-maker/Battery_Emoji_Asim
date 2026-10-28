package com.jhw.batteryemoji.presentation.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jhw.batteryemoji.R
import com.jhw.batteryemoji.databinding.ItemCategoryTabBinding
import com.jhw.batteryemoji.domain.model.CategoryItem

class CategoryTabAdapter(
    initialSelectedId: String = "hot",
    private val onCategoryClick: (CategoryItem) -> Unit
) : RecyclerView.Adapter<CategoryTabAdapter.CategoryViewHolder>() {

    private val categories = mutableListOf<CategoryItem>()
    private var selectedCategoryId: String = initialSelectedId

    fun setCategories(newCategories: List<CategoryItem>) {
        if (categories == newCategories && categories.isNotEmpty()) {
            return
        }
        categories.clear()
        categories.addAll(newCategories)
        notifyDataSetChanged()
    }

    fun setSelectedCategory(categoryId: String) {
        selectedCategoryId = categoryId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryTabBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    inner class CategoryViewHolder(
        private val binding: ItemCategoryTabBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: CategoryItem) {
            val isSelected = category.id == selectedCategoryId
            binding.tvTabTitle.text = if (category.titleRes != 0) binding.root.context.getString(category.titleRes) else category.title

            val context = binding.root.context
            val color = ContextCompat.getColor(context, if (isSelected) R.color.tab_indicator_purple else R.color.tab_inactive)
            binding.tvTabTitle.setTextColor(color)
            binding.ivTabIcon.visibility = if (category.id == "hot") View.VISIBLE else View.GONE
            binding.ivTabIcon.imageTintList = android.content.res.ColorStateList.valueOf(color)
            if (isSelected) {
                binding.tvTabTitle.typeface = android.graphics.Typeface.DEFAULT_BOLD
                binding.vTabIndicator.visibility = View.VISIBLE
            } else {
                binding.tvTabTitle.typeface = android.graphics.Typeface.DEFAULT_BOLD
                binding.vTabIndicator.visibility = View.INVISIBLE
            }

            binding.root.setOnClickListener {
                setSelectedCategory(category.id)
                onCategoryClick(category)
            }
        }
    }
}
