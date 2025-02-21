package com.samyak2403.iptvmine.adapter

    import android.graphics.Typeface
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.ImageView
    import android.widget.LinearLayout
    import android.widget.TextView
    import androidx.core.content.ContextCompat
    import androidx.recyclerview.widget.RecyclerView
    import com.bumptech.glide.Glide
    import com.samyak2403.iptvmine.R

class LanguageAdapter(
        private val languages: List<Language>,
        private val onLanguageSelected: (Language) -> Unit
    ) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

        private var selectedPosition = -1 // Vị trí của item được chọn

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_language, parent, false)
            return LanguageViewHolder(view)
        }

        override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
            val language = languages[position]
            holder.bind(language, position == selectedPosition)

            holder.itemView.setOnClickListener {
                updateSelection(position)
            }

            // Dùng Glide để load ảnh
            Glide.with(holder.itemView.context)
                .load(language.flagResId)
                .circleCrop()
                .into(holder.languageIcon)
        }

        override fun getItemCount(): Int = languages.size

        fun getSelectedLanguage(): Language? {
            return if (selectedPosition != -1) languages[selectedPosition] else null
        }

        private fun updateSelection(newPosition: Int) {
            if (selectedPosition != newPosition) {
                val previousPosition = selectedPosition
                selectedPosition = newPosition
                notifyItemChanged(previousPosition) // Cập nhật radio button cũ (bỏ chọn)
                notifyItemChanged(selectedPosition) // Cập nhật radio button mới (đánh dấu chọn)
            }
        }

        class LanguageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val languageName: TextView = itemView.findViewById(R.id.language_name)
            val languageIcon: ImageView = itemView.findViewById(R.id.flag_icon)
            val imv_btn: ImageView = itemView.findViewById(R.id.imv_btn)
            fun bind(language: Language, isSelected: Boolean) {
                languageName.text = language.name
                languageIcon.setImageResource(language.flagResId)

                itemView.isSelected = isSelected
                itemView.isActivated = isSelected

                imv_btn.setImageResource(if (isSelected) R.drawable.radio_checked else R.drawable.radio_btn)
                languageName.setTextColor(
                    ContextCompat.getColor(itemView.context, if (isSelected) R.color.cool_blue else R.color.black)
                )

                languageName.setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)


            }

        }
    }
