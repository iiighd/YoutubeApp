package com.example.youtubeapp

import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.widget.MediaController
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_add_video.*
import java.util.jar.Manifest

/*this activitywill display list of videos */
class AddVideoActivity : AppCompatActivity() {

    //actions
    private lateinit var actionBar: ActionBar
    private val VIDEO_PICK_GALLERY_CODE = 100
    private val VIDEO_PICK_CAMERA_CODE = 101
    private val CAMERA_REQUEST_CODE = 102
    private lateinit var cameraPermissions: Array<String>
    private var videoUri : Uri? = null
    private  var title:String = "" ;
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_video)
        actionBar = supportActionBar!!
        actionBar.title = "add Video"
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayShowHomeEnabled(true)
        cameraPermissions = arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        //progrees bar
        progressDialog  = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setMessage("Uploading Video .....")
        progressDialog.setCanceledOnTouchOutside(false)
        //handel click -- UPLOAD
        uploadView.setOnClickListener{
            //set title
            title = titleEt.toString().trim()
            if (TextUtils.isEmpty(title)){
                Toast.makeText(this,"Title is requested", Toast.LENGTH_SHORT).show()
            }
            else if (videoUri == null){
                Toast.makeText(this , "pick the Video first", Toast.LENGTH_SHORT).show()
            }
            else {
                uploaVideoFairbase()
            }

        }
        //handel click -- PICK
        pickVideoFab.setOnClickListener {
            videoPickDialog()
        }
    }

    private fun uploaVideoFairbase() {
        //progress dialog show
        progressDialog.show()
        val timestamp = ""+System.currentTimeMillis()
        val filePathName = "Videos/video_\$timestamp"
        val storageReference = FirebaseStorage.getInstance().getReference(filePathName)
        //upload the video to FB storage using uri of the video
        storageReference.putFile(videoUri!!)
            .addOnSuccessListener {  taskSnapshot ->
                val uriTask = taskSnapshot.storage.downloadUrl
                while (!uriTask.isSuccessful);
                val downloadUri = uriTask.result
                if (uriTask.isSuccessful){
                    //video url is rescevied successfuly && add some info to db
                    val hashMap = HashMap<String,Any>()
                    hashMap["id" ]= "$timestamp"
                    hashMap["title"] = "$title"
                    hashMap["timestamp"] = "$timestamp"
                    hashMap["videoUri"] ="$downloadUri"
                    //--
                    val dbReferance = FirebaseDatabase.getInstance().getReference("Videos")
                    dbReferance.child(timestamp)
                        .setValue(hashMap)
                        .addOnSuccessListener { taskSnapshot ->
                            //video iformation added
                            progressDialog.dismiss()
                            Toast.makeText(this , "video uploaded ", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            //failed to add video info
                            progressDialog.dismiss()
                            Toast.makeText(this , "Error!${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                //uploasd
            }
            .addOnFailureListener{ e ->
                //field to upload
                progressDialog.dismiss()
                Toast.makeText(this , "Error!${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setVideoTovideoView() {
        //set the picked video to the vido view
        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView) //back here
        //set media controller , video uri
        videoView.setMediaController(mediaController)
        videoView.setVideoURI(videoUri)
        videoView.requestFocus()
        videoView.setOnPreparedListener {
            //when video is eady by defult dont play automatically -
            videoView.pause() }
    }
    private fun videoPickDialog() {
        val options = arrayOf("Camera","Gallery")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("pick Video from")
            .setItems(options){ dialogInterface, i ->
                if (i==0){
                    //camera clicled
                    if (!checkCameraPermissions()){
                        //permission was not alloweded -- request
                        requestCameraPermissions()
                    }
                    else {
                        //premissiom was allowed
                        videoPickCamera()
                    }
                }
                else {
                    //gallery clickedd
                    videoPickGallery()
                }
            }
            .show()
    }
    private fun requestCameraPermissions(){
        ActivityCompat.requestPermissions(
            this,
            cameraPermissions,
            CAMERA_REQUEST_CODE
        )
    }
    private fun checkCameraPermissions(): Boolean{
        val result1 = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        val result2 = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        return result1 && result2
    }
    private fun videoPickGallery(){
        val intent = Intent()
        intent.type = "video/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            Intent.createChooser(intent, "Choose video"),
            VIDEO_PICK_GALLERY_CODE
        )
    }
    private fun videoPickCamera(){
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        startActivityForResult(intent , VIDEO_PICK_CAMERA_CODE)
    }

    override fun onSupportNavigateUp(): Boolean{
        onBackPressed()
        return super.onSupportNavigateUp()
    }
//handelr permission results --
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    )  {
    //check the indentaions! back here
    when(requestCode){
        CAMERA_REQUEST_CODE ->
            if (grantResults.size >0){
                //check if premission allowed or denied
                val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
                if(cameraAccepted && storageAccepted) {
                    //when both permission is allowed
                    videoPickCamera()
                }
                else {
                    //both or one of these is denied
                    Toast.makeText(this , "Premission denied.", Toast.LENGTH_SHORT).show()
                }
            }
    }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
    //video pick result handler --
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (resultCode == RESULT_OK){
            //video is picked from camera or gallory

            if (requestCode == VIDEO_PICK_CAMERA_CODE){
                //pickd from camra
                videoUri = data!!.data
                setVideoTovideoView()
            }
            else if (requestCode == VIDEO_PICK_GALLERY_CODE){
                //picked from gallery
                videoUri = data!!.data
                setVideoTovideoView()
            }
        }
        else {
            //video picking is canselled
            Toast.makeText(this,"Canceled .", Toast.LENGTH_SHORT).show()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


}