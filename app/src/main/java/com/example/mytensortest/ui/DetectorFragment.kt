package com.example.mytensortest.ui


import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.mytensortest.databinding.FragmentImageBinding


class DetectorFragment : Fragment() {
    private var _binding: FragmentImageBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DetectorViewModel by viewModels()
    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let{
                startDetect(uri)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonAddImage.setOnClickListener {
            pickMedia.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }

        viewModel.bitmapLiveData.observe(viewLifecycleOwner) { bitmap ->
            binding.imageviewDetect.setImageBitmap(bitmap)
        }
        viewModel.progressLiveData.observe(viewLifecycleOwner){progressStatus ->
            when(progressStatus){
                is Loading -> showProgress()
                is Success -> showImage()
            }
        }
    }

    private fun showImage() {
        binding.progressBar.isVisible = false
        binding.imageviewDetect.isVisible = true
    }

    private fun showProgress() {
        binding.imageviewDetect.isVisible = false
        binding.progressBar.isVisible = true
    }

    private fun startDetect(uri: Uri){
        viewModel.startDetect(uri, requireContext())
    }


    companion object {
        const val IMAGE_HEIGHT = 640
        const val IMAGE_WIDTH = 480
        const val MODEL_NAME = "model.tflite"
        const val MODEL_NAME_CACHE = "modelCache.tflite"

        @JvmStatic
        fun newInstance() = DetectorFragment()
    }
}