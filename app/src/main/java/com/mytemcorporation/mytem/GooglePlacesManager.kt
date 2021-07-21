package com.mytemcorporation.mytem

import android.content.Context
import android.graphics.Bitmap
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

class GooglePlacesManager(private val context: Context)
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


    /**
     * Performs a full google places search that invokes the passed in action with the resulting
     * google place, google photo & fetched google place.
     */
    public fun FullGooglePlacesQuery(business: Business, position: LatLng,
                                     onResultAction: (googlePlace: Place, googlePhoto: Bitmap, fetchedPlace: FetchedGooglePlace) -> Unit)
    {
        QueryPlaces(business.business_name, position) {
            val place = it.result!!.autocompletePredictions[0]
            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS,
                Place.Field.OPENING_HOURS, Place.Field.RATING, Place.Field.TYPES, Place.Field.PHOTO_METADATAS)

            QueryDetailsFromPlace(place, fields){
                val placeDetails = it.result!!.place
                val photoMetadata = placeDetails.photoMetadatas!![0]

                QueryPhotoFromPhotoMetaData(photoMetadata) {
                    val placeType = GetPlaceType(context, placeDetails.types!!.toTypedArray())
                    val fetchedGooglePlace = FetchedGooglePlace(business.objectID, business.toString(), placeDetails.id!!, placeDetails.name!!, placeDetails.latLng!!,
                        placeDetails.address!!, placeDetails.openingHours!!, placeDetails.rating!!, placeType,
                        placeDetails.photoMetadatas!!.get(0))

                    onResultAction.invoke(placeDetails, it.result!!.bitmap, fetchedGooglePlace)
                }
            }
        }
    }
}