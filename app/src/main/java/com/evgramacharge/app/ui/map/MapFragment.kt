package com.evgramacharge.app.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.evgramacharge.app.R
import com.evgramacharge.app.data.model.Booking
import com.evgramacharge.app.data.model.ChargingHost
import com.evgramacharge.app.databinding.FragmentMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MapViewModel by viewModels { MapViewModel.factory(requireActivity().application) }

    private var map: GoogleMap? = null
    private val markerByHostId = mutableMapOf<String, Marker>()
    private var didFitBoundsOnce = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            enableMyLocationLayer()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map_container) as SupportMapFragment
        mapFragment.getMapAsync { googleMap ->
            map = googleMap
            googleMap.uiSettings.isZoomControlsEnabled = true
            googleMap.setOnMarkerClickListener { marker ->
                val host = marker.tag as? ChargingHost
                if (host != null) showBookingDialog(host)
                true
            }
            requestLocationIfNeeded()
            binding.fabMyLocation.setOnClickListener { moveToDeviceLocation() }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.hosts.collect { hosts ->
                    renderMarkers(hosts)
                }
            }
        }
    }

    private fun renderMarkers(hosts: List<ChargingHost>) {
        val gMap = map ?: return
        val nextIds = hosts.map { it.id }.toSet()
        markerByHostId.keys.filter { it !in nextIds }.forEach { id ->
            markerByHostId.remove(id)?.remove()
        }
        hosts.forEach { host ->
            if (host.latitude == 0.0 && host.longitude == 0.0) return@forEach
            val position = LatLng(host.latitude, host.longitude)
            val existing = markerByHostId[host.id]
            if (existing != null) {
                existing.position = position
                existing.title = host.name
                existing.snippet = host.address
                existing.tag = host
            } else {
                val marker = gMap.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(host.name)
                        .snippet(host.address)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)),
                )
                marker?.tag = host
                if (marker != null && host.id.isNotBlank()) {
                    markerByHostId[host.id] = marker
                }
            }
        }
        val withCoords = hosts.filter { it.latitude != 0.0 || it.longitude != 0.0 }
        if (!didFitBoundsOnce && withCoords.isNotEmpty()) {
            runCatching {
                if (withCoords.size == 1) {
                    val only = withCoords.first()
                    gMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(only.latitude, only.longitude), 14f),
                    )
                } else {
                    val builder = LatLngBounds.builder()
                    withCoords.forEach { builder.include(LatLng(it.latitude, it.longitude)) }
                    gMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 120))
                }
            }
            didFitBoundsOnce = true
        }
    }

    private fun requestLocationIfNeeded() {
        val fine = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            enableMyLocationLayer()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocationLayer() {
        val gMap = map ?: return
        val fine = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            gMap.isMyLocationEnabled = true
        }
    }

    @SuppressLint("MissingPermission")
    private fun moveToDeviceLocation() {
        val gMap = map ?: return
        val client = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(
            requireActivity(),
        )
        val fine = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            requestLocationIfNeeded()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                val loc = client.lastLocation.await() ?: return@launch
                gMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 14f),
                )
            }
        }
    }

    private fun showBookingDialog(host: ChargingHost) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            Toast.makeText(requireContext(), R.string.sign_in_required, Toast.LENGTH_SHORT).show()
            return
        }

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 0)
        }
        val hoursInput = EditText(requireContext()).apply {
            hint = getString(R.string.duration_hours)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("2")
        }
        val kwhInput = EditText(requireContext()).apply {
            hint = getString(R.string.estimated_kwh_hint)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText("15")
        }
        layout.addView(hoursInput)
        layout.addView(kwhInput)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(host.name)
            .setMessage(
                "${host.address}\n${host.connectorType} · ${host.pricePerKwh} / kWh",
            )
            .setView(layout)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.book_slot) { _, _ ->
                val hours = hoursInput.text.toString().toDoubleOrNull()?.coerceAtLeast(0.5) ?: 2.0
                val kwh = kwhInput.text.toString().toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                submitBooking(host, uid, hours, kwh)
            }
            .show()
    }

    private fun submitBooking(host: ChargingHost, userId: String, hours: Double, kwh: Double) {
        val app = requireActivity().application as com.evgramacharge.app.EVGramaChargeApplication
        val start = System.currentTimeMillis()
        val end = start + (hours * 3_600_000L).toLong()
        val booking = Booking(
            hostId = host.id,
            hostName = host.name,
            userId = userId,
            startEpochMs = start,
            endEpochMs = end.toLong(),
            estimatedEnergyKwh = kwh,
            status = "PENDING",
        )
        viewLifecycleOwner.lifecycleScope.launch {
            binding.mapLoading.visibility = View.VISIBLE
            val result = app.repository.createBooking(booking)
            binding.mapLoading.visibility = View.GONE
            result.onSuccess {
                Toast.makeText(requireContext(), "Booking requested", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(requireContext(), R.string.error_generic, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        markerByHostId.clear()
        map = null
        _binding = null
    }
}
