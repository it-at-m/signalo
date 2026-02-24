package com.example.test.signalo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.example.test.signalo.databinding.WelcomeDialogBinding
import com.example.test.signalo.utils.Constants

class WelcomeDialogFragment : DialogFragment() {
    private lateinit var _binding: WelcomeDialogBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = WelcomeDialogBinding.inflate(inflater, container, false)

        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return _binding.root
    }

    override fun onStart() {
        //set layout of welcome dialog to prevent fullscreen
        super.onStart()
        dialog?.window?.apply {
            val width = (resources.displayMetrics.widthPixels * 0.95).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding.btnStart.setOnClickListener {
            val result = Bundle().apply {
                putBoolean("start_clicked", true)
            }
            setFragmentResult(Constants.DIALOG_REQUEST_KEY, result)

            dismiss()
        }
        _binding.btnCancel.setOnClickListener {

            dismiss()
        }
    }
}

