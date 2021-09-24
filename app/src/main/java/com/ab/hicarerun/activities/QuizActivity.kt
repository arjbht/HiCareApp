package com.ab.hicarerun.activities;

import android.content.DialogInterface
import android.os.Build
import android.os.Bundle;
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.ab.hicarerun.BaseActivity;
import com.ab.hicarerun.R;
import com.ab.hicarerun.databinding.ActivityQuizBinding;
import com.ab.hicarerun.fragments.QuizFragment

class QuizActivity : BaseActivity() {

    lateinit var binding: ActivityQuizBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val window = window
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimary)
        }

        val intent = intent
        val puzzleId = intent.getIntExtra("puzzleId", -1)
        val puzzleTitle = intent.getStringExtra("puzzleTitle").toString()
        binding.titleTv.text = "${R.string.shiksha} - $puzzleTitle"
        replaceFragment(QuizFragment.newInstance(puzzleId, puzzleTitle), "QuizActivity - QuizFragment")
    }

    private fun showQuitDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Quiz Alert")
        builder.setMessage("Are you sure you want to quit the game?")
        builder.setPositiveButton("Quit") { dialog: DialogInterface, which: Int ->
            dialog.cancel()
            finish()
        }
        builder.setNegativeButton("Cancel") { dialog: DialogInterface, which: Int -> dialog.cancel() }
        builder.show()
    }
    override fun onBackPressed() {
        //showQuitDialog()
    }
}

