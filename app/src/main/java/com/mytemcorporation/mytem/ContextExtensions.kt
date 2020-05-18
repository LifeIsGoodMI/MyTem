package com.mytemcorporation.mytem

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import androidx.appcompat.widget.AppCompatImageView
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


public fun GetPlaceType(context: Context, types: List<Place.Type>) : String
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