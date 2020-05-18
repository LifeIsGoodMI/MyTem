package com.mytemcorporation.mytem

import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.OpeningHours
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.android.material.shape.ShapePath
import kotlinx.android.parcel.Parcelize
import kotlinx.serialization.Serializable

@Serializable @Parcelize
data class Product(
    val productname: String,
    val refID: String,
    val imageUrl: String,
    val objectID: String
) : Parcelable

@Serializable @Parcelize
data class Business(
    val business_name: String,
    val postal_code: String,
    val products: Array<String>,
    val objectID: String,
    val _rankingInfo: RankingInfo
) : Parcelable
{
    override fun toString(): String
    {
        val dist = _rankingInfo.matchedGeoLocation.distance.toString() + "m"
        return "$business_name, $postal_code, $dist"
    }
}

@Serializable @Parcelize
data class MatchedGeoLocation(
    val lat: Double,
    val lng: Double,
    val distance: Int
) : Parcelable

@Serializable @Parcelize
data class RankingInfo(
    val nbTypos: Int,
    val firstMatchedWord: Int,
    val proximityDistance: Int,
    val userScore: Int,
    val geoDistance: Int,
    val geoPrecision: Int,
    val nbExactWords: Int,
    val words: Int,
    val filters: Int,
    val matchedGeoLocation: MatchedGeoLocation
) : Parcelable

@Parcelize
data class FetchedGooglePlace(
    val objectID: String,
    val title: String,
    val id: String,
    val name: String,
    val latLng: LatLng,
    val address: String,
    val openingHours: OpeningHours,
    val rating: Double,
    val type: String,
    val photoMetadata: PhotoMetadata?
) : Parcelable

@Serializable @Parcelize
data class ShoppingList(
    val description: String,
    val products: Array<Product>
) : Parcelable