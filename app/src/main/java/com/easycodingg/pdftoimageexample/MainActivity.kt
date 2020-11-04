package com.easycodingg.pdftoimageexample

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.Exception

class MainActivity : AppCompatActivity() {

    companion object {
        private const val INTENT_REQUEST_CODE = 0
    }

    private var selectedPdfUri: Uri? = null
    private var selectedPdfName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnConvertToImages.setOnClickListener {
            convertPdfToImages()
        }

    }

    private fun convertPdfToImages() {
        Log.d("Pathy","Inside convert function")
        if(selectedPdfUri != null){
            CoroutineScope(Dispatchers.IO).launch {
                Log.d("Pathy","Convert function Starting")
                val filePath = selectedPdfUri!!.path
                val parcelFileDescriptor = contentResolver.openFileDescriptor(selectedPdfUri!!, "r")

                if(parcelFileDescriptor != null){
                    val renderer = PdfRenderer(parcelFileDescriptor)

                    val pageCount = renderer.pageCount

                    for(pageIndex in 0 until pageCount) {
                        Log.d("Pathy","Rendering Pages")
                        val page = renderer.openPage(pageIndex)
                        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)

                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)

                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        page.close()
                        saveImages(bitmap, (pageIndex + 1).toString())
                    }

                    renderer.close()
                }
            }
        }
    }

    private suspend fun saveImages(finalBitmap: Bitmap, fileName: String) {
        val dirName = getFileNameWithoutExtension(selectedPdfName!!)

        if(!getExternalFilesDir(dirName)?.exists()!!){
            getExternalFilesDir(dirName)?.mkdir()
        }

        val filePath = getExternalFilesDir(null)?.path + "/$dirName/$fileName.jpg"

        val file = File(filePath)

        try {
            val outputStream = FileOutputStream(file)
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

        } catch(e: Exception){
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getFileNameWithoutExtension(fileName: String): String {
        return fileName.substring(0, fileName.lastIndexOf("."))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.miSelectImages -> {
                Intent(Intent.ACTION_GET_CONTENT).also {
                    it.type = "application/pdf"
                    startActivityForResult(it, INTENT_REQUEST_CODE)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if(resultCode == Activity.RESULT_OK && requestCode == INTENT_REQUEST_CODE){
            intent?.data.let {
                selectedPdfUri = it
                selectedPdfName = File(it?.path!!).name
                tvPdfName.text = selectedPdfName
                tvPdfName.visibility = View.VISIBLE
                ivPdfSymbol.visibility = View.VISIBLE
            }
        }
    }
}