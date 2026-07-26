package com.belovedfx.pricecheck

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.belovedfx.pricecheck.databinding.ActivityAddProductBinding
import java.io.File
import java.util.*

class AddProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProductBinding
    private var photoUri: Uri? = null
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            Glide.with(this).load(photoUri).centerCrop().into(binding.photoPreview)
        }
    }

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera() else
            Toast.makeText(this, "Camera permission is needed to add a photo", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.closeButton.setOnClickListener { finish() }

        binding.photoPreview.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                requestCameraPermission.launch(android.Manifest.permission.CAMERA)
            }
        }

        binding.saveButton.setOnClickListener { saveProduct() }
    }

    private fun launchCamera() {
        val photoFile = File(cacheDir, "product_${System.currentTimeMillis()}.jpg")
        photoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile)
        takePicture.launch(photoUri)
    }

    private fun saveProduct() {
        val name = binding.nameInput.text.toString().trim()
        val priceText = binding.priceInput.text.toString().trim()
        val price = priceText.toDoubleOrNull()

        if (name.isEmpty()) {
            binding.nameInput.error = "Required"
            return
        }
        if (price == null || price < 0) {
            binding.priceInput.error = "Enter a valid price"
            return
        }

        binding.saveButton.isEnabled = false
        binding.savingProgress.visibility = android.view.View.VISIBLE

        if (photoUri != null) {
            val ref = storage.reference.child("product_images/${UUID.randomUUID()}.jpg")
            ref.putFile(photoUri!!)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { url ->
                        writeToFirestore(name, price, url.toString())
                    }
                }
                .addOnFailureListener { onSaveFailed() }
        } else {
            writeToFirestore(name, price, null)
        }
    }

    private fun writeToFirestore(name: String, price: Double, imageUrl: String?) {
        val product = hashMapOf(
            "name" to name,
            "price" to price,
            "previousPrice" to null,
            "priceUpdatedAt" to System.currentTimeMillis(),
            "imageUrl" to imageUrl,
            "addedAt" to System.currentTimeMillis()
        )
        db.collection("products").add(product)
            .addOnSuccessListener { finish() }
            .addOnFailureListener { onSaveFailed() }
    }

    private fun onSaveFailed() {
        binding.saveButton.isEnabled = true
        binding.savingProgress.visibility = android.view.View.GONE
        Toast.makeText(this, "Couldn't save. Check your connection and try again.", Toast.LENGTH_SHORT).show()
    }
}
