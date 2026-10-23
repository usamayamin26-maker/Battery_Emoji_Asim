package com.asim.jhbatteryapp.presentation.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.databinding.ItemCategoryTabBinding
import com.asim.jhbatteryapp.domain.model.CategoryItem

class CategoryTabAdapter(
    private val onCategoryClick: (CategoryItem) -> Unit
) : RecyclerView.Adapter<CategoryTabAdapter.CategoryViewHolder>() {

    private val categories = mutableListOf<CategoryItem>()
    private var selectedCategoryId: String = "hot"

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
            if (isSelected) {
                binding.tvTabTitle.setTextColor(ContextCompat.getColor(context, R.color.tab_indicator_purple))
                binding.tvTabTitle.typeface = android.graphics.Typeface.DEFAULT_BOLD
                binding.vTabIndicator.visibility = View.VISIBLE
            } else {
                binding.tvTabTitle.setTextColor(ContextCompat.getColor(context, R.color.tab_inactive_text))
                binding.tvTabTitle.typeface = android.graphics.Typeface.DEFAULT
                binding.vTabIndicator.visibility = View.INVISIBLE
            }

            binding.root.setOnClickListener {
                setSelectedCategory(category.id)
                onCategoryClick(category)
            }
        }
    }
}
