package com.mytemcorporation.mytem

import android.content.Intent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest

class GoogleMapsHandler(val boundActivity: AppCompatActivity, val googlePlacesManager: GooglePlacesManager, val mapFragmentId: Int) : OnMapReadyCallback, GoogleMap.OnMapLoadedCallback
{
    companion object
    {
        private const val SingleMarkerDefaultZoom = 12.0f
    }

    private lateinit var map: GoogleMap                                         // The actual google map.
    private lateinit var mapFragment: SupportMapFragment                        // The UI map fragment.

    private var scheduledActions: MutableList<() -> Unit> = mutableListOf()     // Scheduled calls to CenterMapOnMarkers. CenterMapOnMarkers might be called
                                                                                // before the google map has been loaded, so these need to be scheduled.
    private lateinit var fetchedPlaces: Array<FetchedGooglePlace>               // Caches the details of fetched places when creating markers.

    private lateinit var boundsBuilder: LatLngBounds.Builder                    // The bounds builder to compute the bounds containing all set markers.
    private  var markers: MutableList<Marker> = mutableListOf()                 // Per-business created markers.

    public var CenterMapOnMarkersAction: () -> Unit = ::CenterMapOnMarkers      // Exposes the CenterMapOnMarkers function for scheduling.

    init
    {
        SetGoogleMap()
    }

    override fun onMapReady(googleMap: GoogleMap)
    {
        map = googleMap

        map.isMyLocationEnabled = true
        map.setOnMapLoadedCallback(this)
    }

    override fun onMapLoaded()
    {
        for (action in scheduledActions)
        {
            action.invoke()
        }
    }


    private fun SetGoogleMap()
    {
        mapFragment = boundActivity.supportFragmentManager.findFragmentById(mapFragmentId) as SupportMapFragment
        mapFragment.getMapAsync(this)

        RebindGoogleMapsToolbarButtons(mapFragment.view!!)
    }


    public fun ScheduleAction(action: () -> Unit, parameters: Array<FetchedGooglePlace>)
    {
        scheduledActions.add(action)
        fetchedPlaces = parameters
    }


    private fun CenterMapOnMarkers()
    {
        map.clear()
        markers.clear()
        boundsBuilder = LatLngBounds.builder()

        for (fetchedGooglePlaces in fetchedPlaces)
        {
            SetMarker(fetchedGooglePlaces)
        }

        if (fetchedPlaces.count() == 1)
        {
            val bounds = boundsBuilder.build()
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(bounds.center, SingleMarkerDefaultZoom))
        }
        else
        {
            val bounds = boundsBuilder.build()
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 300))
        }
    }

    private fun SetMarker(fetchedGooglePlace: FetchedGooglePlace)
    {
        val markerOptions = MarkerOptions()
        markerOptions.title(fetchedGooglePlace.title)
        markerOptions.position(fetchedGooglePlace.latLng)
        val marker = map.addMarker(markerOptions)
        markers.add(marker)

        boundsBuilder.include(fetchedGooglePlace.latLng)
    }


    private fun RebindGoogleMapsToolbarButtons(view: View)
    {
        val directionsButton = view.findViewWithTag<View>("GoogleMapDirectionsButton")
        val openInMapsButton = view.findViewWithTag<View>("GoogleMapOpenGmmButton")

        directionsButton.setOnClickListener(object : View.OnClickListener
        {
            override fun onClick(v: View?)
            {
                OpenMapsAppFromSelectedMarker()
            }
        })

        openInMapsButton.setOnClickListener(object : View.OnClickListener
        {
            override fun onClick(v: View?)
            {
                OpenMapsAppFromSelectedMarker()
            }
        })
    }

    private fun OpenMapsAppFromSelectedMarker()
    {
        val selectedMarker = markers.find { m -> m.isInfoWindowShown }
        val fetchedPlace = fetchedPlaces.find { place -> place!!.latLng == selectedMarker!!.position }

        OpenMapsAppFromUri(fetchedPlace!!)
    }

    public fun OpenMapsAppFromUri(fetchedPlace: FetchedGooglePlace)
    {
        val query = fetchedPlace.name + "%2C+" + fetchedPlace.address
        val uriString = "https://www.google.com/maps/search/?api=1&query=${query}"
        val uri = android.net.Uri.parse(uriString)

        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)

        boundActivity.startActivity(intent)
    }
}