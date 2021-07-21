package com.mytemcorporation.mytem

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import com.algolia.search.client.ClientSearch
import com.algolia.search.client.Index
import com.algolia.search.model.ApplicationID
import com.algolia.search.model.IndexName
import com.algolia.search.model.search.Point
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.OpeningHours
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.maps.android.SphericalUtil
import java.util.*
import kotlin.math.sqrt

public fun HideAndroidKeyboard(view: View)
{
    val inputManager = view.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputManager.hideSoftInputFromWindow(view.windowToken, 0)
}

public fun GetRectBoundsFromCircle(center: LatLng, radius: Int) : RectangularBounds
{
    val centerToCorner = radius * sqrt(2.0)

    val southWestCorner = SphericalUtil.computeOffset(center, centerToCorner, 225.0)
    val northEastCorner = SphericalUtil.computeOffset(center, centerToCorner, 45.0)
    return RectangularBounds.newInstance(southWestCorner, northEastCorner)
}

// R.id.search_btn_close returns the wrong ID, resulting in a null ref exception.
// This helper function simplifies access to a search views close button.
public fun GetSearchViewCloseButton(searchView: SearchView) : AppCompatImageView
{
    val searchCloseButtonId = searchView.context.resources.getIdentifier("android:id/search_close_btn", null, null)
    val closeButton = searchView.findViewById<AppCompatImageView>(searchCloseButtonId)
    return closeButton
}

//region Place opening
public fun GetGoogleDayToday() : Int
{
    // Calendar.DAY_OF_WEEK returns 1 for Sunday
    // while OpeningHours should return 0 for Monday. (NOT GUARANTEED)
    val calendarDay = Calendar.getInstance(Locale.GERMANY).get(Calendar.DAY_OF_WEEK)
    val day = if(calendarDay == 1)  6 else calendarDay - 2
    return day;
}

public fun FormatOpeningHoursToday(context: Context, openingHours: OpeningHours) : String
{
    val day = GetGoogleDayToday()
    val openingHoursToday = openingHours.weekdayText[day]
    val openingHoursStrings = openingHoursToday.split(":")
    val trimmedOpeningHours = openingHoursToday.removeRange(0, openingHoursStrings[0].length + 2)

    val prefix = context.resources.getString(R.string.business_opening_hours_today_prefix) + ": "
    return prefix + trimmedOpeningHours
}

public fun IsGooglePlaceCurrentlyOpen(context: Context, openingHours: OpeningHours) : Boolean
{
    val day = GetGoogleDayToday()
    val closedText = context.resources.getString(R.string.business_closed_today)
    return !openingHours.weekdayText[day].contains(closedText)
}

public fun SortBusinessesByOpeningHours(context: Context, businesses: Array<Business>, googlePlaces: Array<FetchedGooglePlace>) : Array<Business>
{
    var sortedBusinesses = businesses.toMutableList()
    var closedBusinesses = mutableListOf<Business>()
    for (place in googlePlaces)
    {
        if (IsGooglePlaceCurrentlyOpen(context, place.openingHours))
            continue

        val business = businesses.find { b -> b.objectID == place.objectID }
        sortedBusinesses.remove(business)
        closedBusinesses.add(business!!)
    }

    closedBusinesses.sortBy { it._rankingInfo.matchedGeoLocation.distance }
    sortedBusinesses.addAll(closedBusinesses)

    return sortedBusinesses.toTypedArray()
}
//endregion

public fun GetPlaceType(context: Context, types: Array<Place.Type>) : String
{
    if (types.contains(Place.Type.SUPERMARKET))
        return context.resources.getString(R.string.place_type_supermarket)
    else if(types.contains(Place.Type.DRUGSTORE))
        return context.resources.getString(R.string.place_type_drug_store)
    else if(types.contains(Place.Type.LIQUOR_STORE))
        return context.resources.getString(R.string.place_type_liquor_store)
    else if(types.contains(Place.Type.CONVENIENCE_STORE))
        return context.resources.getString(R.string.place_type_convenience_store)

    return context.resources.getString(R.string.place_type_store)
}

//region Location Services
public fun HasLocationPermissions(context: Context) : Boolean
{
    val coarseLocationPermissionGranted = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val fineLocationPermissionGranted = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (coarseLocationPermissionGranted && fineLocationPermissionGranted)
        return true

    return false
}

@SuppressLint("MissingPermission")
public fun GetDeviceLocation(context: Context) : Point
{
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val locationProvider = locationManager.allProviders[0]
    val location = locationManager.getLastKnownLocation(locationProvider)
    val locationPoint = Point(location.latitude.toFloat(), location.longitude.toFloat())

    return locationPoint
}
//endregion

//region Algolia
public fun InitAlgolia(indexName: String) : Index
{
    val appID = ApplicationID(AlgoliaAppID)
    val apiKey = com.algolia.search.model.APIKey(AlgoliaAPIKey)
    val indexName = IndexName(indexName)

    val searchClient = ClientSearch(appID, apiKey)
    val index = searchClient.initIndex(indexName)
    return index
}
//endregion