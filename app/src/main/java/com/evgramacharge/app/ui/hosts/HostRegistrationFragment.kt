package com.evgramacharge.app.ui.hosts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.evgramacharge.app.databinding.FragmentHostRegistrationBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class HostRegistrationFragment : Fragment() {

    private var _binding: FragmentHostRegistrationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HostRegistrationViewModel by viewModels {
        HostRegistrationViewModel.factory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHostRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.inputConnector.setText("Type 2")

        binding.btnSaveHost.setOnClickListener {
            val name = binding.inputName.text?.toString().orEmpty()
            val address = binding.inputAddress.text?.toString().orEmpty()
            val lat = binding.inputLat.text?.toString()?.toDoubleOrNull() ?: 0.0
            val lng = binding.inputLng.text?.toString()?.toDoubleOrNull() ?: 0.0
            val price = binding.inputPrice.text?.toString()?.toDoubleOrNull() ?: 0.0
            val connector = binding.inputConnector.text?.toString().orEmpty()
            binding.saveProgress.visibility = View.VISIBLE
            binding.btnSaveHost.isEnabled = false
            viewModel.saveHost(name, address, lat, lng, price, connector)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        HostRegistrationViewModel.UiEvent.Saved -> {
                            binding.saveProgress.visibility = View.GONE
                            binding.btnSaveHost.isEnabled = true
                            Toast.makeText(requireContext(), "Host saved", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        }
                        is HostRegistrationViewModel.UiEvent.Error -> {
                            binding.saveProgress.visibility = View.GONE
                            binding.btnSaveHost.isEnabled = true
                            Snackbar.make(binding.root, event.message, Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
