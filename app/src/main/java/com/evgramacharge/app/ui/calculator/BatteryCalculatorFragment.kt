package com.evgramacharge.app.ui.calculator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.evgramacharge.app.databinding.FragmentBatteryCalculatorBinding
import com.google.android.material.snackbar.Snackbar
import java.util.Locale

class BatteryCalculatorFragment : Fragment() {

    private var _binding: FragmentBatteryCalculatorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BatteryCalculatorViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentBatteryCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.inputBatteryKwh.setText("60")
        binding.inputCurrentSoc.setText("20")
        binding.inputTargetSoc.setText("80")
        binding.inputConsumption.setText("16")

        binding.btnCalculate.setOnClickListener {
            val battery = binding.inputBatteryKwh.text.toString().toDoubleOrNull() ?: 0.0
            val cur = binding.inputCurrentSoc.text.toString().toDoubleOrNull() ?: 0.0
            val target = binding.inputTargetSoc.text.toString().toDoubleOrNull() ?: 0.0
            val cons = binding.inputConsumption.text.toString().toDoubleOrNull() ?: 0.0
            val result = viewModel.calculate(battery, cur, target, cons)
            if (result == null) {
                Snackbar.make(binding.root, "Check inputs (target must exceed current).", Snackbar.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            binding.resultEnergy.text =
                String.format(Locale.getDefault(), "%.2f kWh", result.energyKwh)
            binding.resultRange.text =
                String.format(Locale.getDefault(), "%.0f km", result.extraRangeKm)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
