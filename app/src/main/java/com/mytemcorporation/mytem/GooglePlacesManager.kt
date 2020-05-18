package com.mytemcorporation.mytem

import android.content.Context
import com.algolia.search.model.places.PlacesQuery
import com.google.android.gms.common.api.GoogleApi
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.LocationBias
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.*
import kotlinx.coroutines.*

class GooglePlacesManager(context: Context)
{
    private lateinit var placesClient: PlacesClient
    init
    {
        Places.initialize(context, APIKey)
        placesClient = Places.createClient(context)
    }


    public fun QueryPlaces(placeName: String, position: LatLng, onCompleteAction: (result: Task<FindAutocompletePredictionsResponse>) -> Unit)
    {
        val request = FindAutocompletePredictionsRequest.builder()
        val locationBias = GetRectBoundsFromCircle(position, 100)
        request.setLocationBias(locationBias)
        request.setQuery(placeName)
        request.setCountry(GermanyIsoCode)

        val task = placesClient.findAutocompletePredictions(request.build())
        task.addOnCompleteListener {
            onCompleteAction(it)
        }
    }

    public fun QueryDetailsFromPlace(place: AutocompletePrediction, fields: List<Place.Field>, onCompleteAction: (result: Task<FetchPlaceResponse>) -> Unit)
    {
        val request = FetchPlaceRequest.builder(place.placeId, fields)

        val task = placesClient.fetchPlace(request.build())
        task.addOnCompleteListener {
            onCompleteAction(it)
        }
    }

    public fun QueryPhotoFromPhotoMetaData(photoMetadata: PhotoMetadata, maxWidth: Int = 200, maxHeight: Int = 200, onCompleteAction: (result: Task<FetchPhotoResponse>) -> Unit)
    {
        val request = FetchPhotoRequest.builder(photoMetadata)
        request.setMaxWidth(maxWidth)
        request.setMaxHeight(maxHeight)

        val task = placesClient.fetchPhoto(request.build()).addOnCompleteListener {
            onCompleteAction(it)
        }
    }
}