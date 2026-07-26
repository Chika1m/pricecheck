package com.belovedfx.pricecheck

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class ProductAdapter(
    private val onEditPrice: (Product, Double) -> Unit,
    private val onDelete: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.VH>() {

    private val items = mutableListOf<Product>()

    fun submitList(newItems: List<Product>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun getItemAt(position: Int): Product = items[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return VH(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image: ImageView = itemView.findViewById(R.id.productImage)
        private val name: TextView = itemView.findViewById(R.id.productName)
        private val price: TextView = itemView.findViewById(R.id.productPrice)
        private val previousPriceText: TextView = itemView.findViewById(R.id.previousPriceText)
        private val trendArrow: ImageView = itemView.findViewById(R.id.trendArrow)
        private val deleteBtn: ImageButton = itemView.findViewById(R.id.deleteButton)
        private val card: CardView = itemView.findViewById(R.id.card)

        fun bind(product: Product) {
            name.text = product.name
            price.text = "$" + String.format(Locale.US, "%,.2f", product.price)

            if (product.imageUrl != null) {
                Glide.with(itemView).load(product.imageUrl).centerCrop().into(image)
            } else {
                image.setImageResource(R.drawable.ic_placeholder)
            }

            if (product.previousPrice != null) {
                val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                val when_ = if (product.priceUpdatedAt > 0)
                    sdf.format(Date(product.priceUpdatedAt)) else ""
                previousPriceText.visibility = View.VISIBLE
                previousPriceText.text = "was $" +
                    String.format(Locale.US, "%,.2f", product.previousPrice) + " · $when_"

                trendArrow.visibility = View.VISIBLE
                if (product.price > product.previousPrice) {
                    trendArrow.setImageResource(R.drawable.ic_arrow_up)
                } else if (product.price < product.previousPrice) {
                    trendArrow.setImageResource(R.drawable.ic_arrow_down)
                } else {
                    trendArrow.visibility = View.GONE
                }
            } else {
                previousPriceText.visibility = View.GONE
                trendArrow.visibility = View.GONE
            }

            price.setOnClickListener {
                showEditPriceDialog(itemView, product)
            }

            deleteBtn.setOnClickListener {
                onDelete(product)
            }
        }

        private fun showEditPriceDialog(itemView: View, product: Product) {
            val context = itemView.context
            val input = EditText(context).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                setText(String.format(Locale.US, "%.2f", product.price))
                setSelectAllOnFocus(true)
            }
            val padding = (16 * context.resources.displayMetrics.density).toInt()
            val container = FrameLayout(context).apply {
                setPadding(padding, padding / 2, padding, 0)
                addView(input)
            }

            AlertDialog.Builder(context)
                .setTitle("Update price — ${product.name}")
                .setView(container)
                .setPositiveButton("Save") { _, _ ->
                    val newPrice = input.text.toString().toDoubleOrNull()
                    if (newPrice != null && newPrice >= 0) {
                        onEditPrice(product, newPrice)
                    } else {
                        Toast.makeText(context, "Enter a valid price", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
