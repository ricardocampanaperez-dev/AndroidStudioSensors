package com.example.androidstudio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.androidstudio.databinding.FragmentContactBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class ContactFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentContactBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val studyCenter = LatLng(37.6056, -0.9913)
        googleMap.addMarker(
            MarkerOptions()
                .position(studyCenter)
                .title("Biblioteca Central StudyFlip")
                .snippet("Horario: 08:30 - 21:00")
        )
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(studyCenter, 15f))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}