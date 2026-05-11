package com.evgramacharge.app.ui.hosts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.evgramacharge.app.R
import com.evgramacharge.app.databinding.FragmentHostsBinding
import kotlinx.coroutines.launch

class HostsFragment : Fragment() {

    private var _binding: FragmentHostsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HostsViewModel by viewModels {
        HostsViewModel.factory(requireActivity().application)
    }

    private val adapter = MyHostsAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerMyHosts.adapter = adapter
        binding.fabRegisterHost.setOnClickListener {
            findNavController().navigate(R.id.hostRegistrationFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.myHosts.collect { list ->
                    adapter.submitList(list)
                    binding.emptyHosts.visibility =
                        if (list.isEmpty()) View.VISIBLE else View.GONE
                    binding.recyclerMyHosts.visibility =
                        if (list.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
