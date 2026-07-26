package com.belovedfx.pricecheck

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.belovedfx.pricecheck.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: ProductAdapter
    private var allProducts = listOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ProductAdapter(
            onEditPrice = { product, newPrice -> updatePrice(product, newPrice) },
            onDelete = { product -> confirmDelete(product) }
        )

        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = adapter

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, AddProductActivity::class.java))
        }

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                filterList(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        listenForProducts()
    }

    private fun listenForProducts() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        db.collection("products")
            .orderBy("addedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                binding.progressBar.visibility = android.view.View.GONE
                if (error != null || snapshot == null) {
                    Toast.makeText(this, "Couldn't load products. Check your connection.", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                allProducts = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Product::class.java)?.apply { id = doc.id }
                }
                filterList(binding.searchInput.text?.toString().orEmpty())
            }
    }

    private fun filterList(query: String) {
        val filtered = if (query.isBlank()) allProducts
            else allProducts.filter { it.name.contains(query, ignoreCase = true) }
        adapter.submitList(filtered)
        binding.emptyState.visibility =
            if (filtered.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        binding.emptyState.text = if (allProducts.isEmpty())
            "No products yet. Tap + to add the first one." else "No products match your search."
        binding.countLabel.text = "${allProducts.size} item${if (allProducts.size == 1) "" else "s"}"
    }

    private fun updatePrice(product: Product, newPrice: Double) {
        if (newPrice == product.price) return
        val updates = mapOf(
            "previousPrice" to product.price,
            "price" to newPrice,
            "priceUpdatedAt" to System.currentTimeMillis()
        )
        db.collection("products").document(product.id).update(updates)
            .addOnFailureListener {
                Toast.makeText(this, "Couldn't save the new price. Try again.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmDelete(product: Product) {
        AlertDialog.Builder(this)
            .setTitle("Remove ${product.name}?")
            .setMessage("This removes it for everyone on the shared list.")
            .setPositiveButton("Remove") { _, _ ->
                db.collection("products").document(product.id).delete()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
