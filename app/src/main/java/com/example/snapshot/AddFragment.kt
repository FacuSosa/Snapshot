package com.example.snapshot

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.example.snapshot.databinding.FragmentAddBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class AddFragment : Fragment() {

    private val PATH_SNAPSHOT = "snapshots"

    private lateinit var binding: FragmentAddBinding
    private lateinit var mStorageReference: StorageReference
    private lateinit var mDatabaseReference: DatabaseReference

    private var mPhotoSelectUri: Uri? = null
    private val galleryResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            mPhotoSelectUri = it.data?.data

            with(binding) {
                imgPhoto.setImageURI(mPhotoSelectUri)
                tiltitle.visibility = View.VISIBLE
                tvMessage.text = getString(R.string.post_message_valid_title)
            }

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       binding = FragmentAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnPost.setOnClickListener { postSnapshot() }

        binding.btnSelect.setOnClickListener { openGallery() }

        mStorageReference = FirebaseStorage.getInstance().reference
        mDatabaseReference = FirebaseDatabase.getInstance().reference.child(PATH_SNAPSHOT)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryResult.launch(intent)
    }

    private fun postSnapshot() {
        binding.progressBar.visibility = View.VISIBLE
        val key = mDatabaseReference.push().key!!
        val storageReference = mStorageReference.child(PATH_SNAPSHOT)
            .child(FirebaseAuth.getInstance().currentUser!!.uid).child(key)
        if (mPhotoSelectUri != null) {
            storageReference.putFile(mPhotoSelectUri!!)
                .addOnProgressListener {
                    val progress = (100 * it.bytesTransferred/it.totalByteCount).toDouble()
                    binding.progressBar.progress = progress.toInt()
                    binding.tvMessage.text = "$progress%"
                }
                .addOnCompleteListener {
                    binding.progressBar.visibility = View.INVISIBLE
                }
                .addOnSuccessListener {
                    Snackbar.make(binding.root, "Instantánea publicada.",
                        Snackbar.LENGTH_SHORT).show()
                    it.storage.downloadUrl.addOnSuccessListener {
                        saveSnapshot(key, it.toString(), binding.etTitle.text.toString().trim())
                        binding.tiltitle.visibility = View.GONE
                        binding.tvMessage.text = getString(R.string.post_message_title)
                    }
                }
                .addOnFailureListener {
                    Snackbar.make(binding.root, "No se pudo subir, intente más tarde.",
                        Snackbar.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveSnapshot(key: String, url: String, title: String){
        val snapshot = Snapshot(title = title, photoUrl = url, )
        mDatabaseReference.child(key).setValue(snapshot)
    }


}