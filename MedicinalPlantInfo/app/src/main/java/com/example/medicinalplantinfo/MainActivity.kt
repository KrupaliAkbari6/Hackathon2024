package com.example.medicinalplantinfo

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

class MainActivity : AppCompatActivity() {

    private val CAMERA_REQUEST_CODE = 101
    private val GALLERY_REQUEST_CODE = 102
    private lateinit var selectedImageBitmap: Bitmap
    private lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val selectImageButton: Button = findViewById(R.id.selectImageButton)
        val scanButton: Button = findViewById(R.id.scanButton)
        imageView = findViewById(R.id.imageView)

        selectImageButton.setOnClickListener {
            showImageSourceDialog()
        }

        scanButton.setOnClickListener {
            if (::selectedImageBitmap.isInitialized) {
                processImage(selectedImageBitmap)
            } else {
                Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val storagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        val listPermissionsNeeded = mutableListOf<String>()

        if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA)
        }
        if (storagePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toTypedArray(), 0)
            return false
        }
        return true
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take a Photo", "Open Gallery", "Cancel")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Image Source")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> {
                    if (checkAndRequestPermissions()) {
                        openCamera()
                    }
                }
                1 -> {
                    if (checkAndRequestPermissions()) {
                        openGallery()
                    }
                }
                2 -> dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showImageSourceDialog()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST_CODE -> {
                    val bitmap = data?.extras?.get("data") as Bitmap
                    selectedImageBitmap = bitmap
                    imageView.setImageBitmap(selectedImageBitmap)
                }
                GALLERY_REQUEST_CODE -> {
                    val imageUri: Uri? = data?.data
                    val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
                    selectedImageBitmap = bitmap
                    imageView.setImageBitmap(selectedImageBitmap)
                }
            }
        }
    }

    // ML Kit image recognition using Image Labeling API
    private fun processImage(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        labeler.process(image)
            .addOnSuccessListener { labels ->
                val isPlantImage = recognizePlant(labels)
                if (isPlantImage) {
                    showRecognizedPlantPopup()
                } else {
                    showIrrelevantImagePopup()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error recognizing image", Toast.LENGTH_SHORT).show()
            }
    }

    private fun recognizePlant(labels: List<ImageLabel>): Boolean {
        // Loop through labels to check if it contains plant-related keywords
        for (label in labels) {
            if (label.text.equals("plant", true) || label.text.equals("leaf", true) ||
                label.text.equals("tree", true) || label.text.equals("flower", true)) {
                return true
            }
        }
        return false
    }

    private fun showRecognizedPlantPopup() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Plant Recognized")
        builder.setMessage("This image contains a plant.")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun showIrrelevantImagePopup() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Irrelevant Image")
        builder.setMessage("This image does not contain a plant.")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }
}
