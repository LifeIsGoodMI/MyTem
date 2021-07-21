package com.mytemcorporation.mytem.adapters

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.mytemcorporation.mytem.Business
import com.mytemcorporation.mytem.FetchedGooglePlace
import com.mytemcorporation.mytem.IsGooglePlaceCurrentlyOpen
import com.mytemcorporation.mytem.R

class BusinessAdapter(private var context: Context, private var items: Array<Business>, private var layout: Int, private var elementId: Int) : BaseAdapter()
{
    private var fetchedPlaces: Array<FetchedGooglePlace> = emptyArray()
    private var placesImages: HashMap<String, Bitmap?> = HashMap()

    override fun getItem(position: Int): Business
    {
        return items[position]
    }

    override fun getItemId(position: Int): Long
    {
        return position.toLong()
    }

    override fun getCount(): Int
    {
        return items.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View
    {
        var row = convertView;
        if (row == null)
        {
            row = LayoutInflater.from(context).inflate(layout, null, false);
        }

        val fetchedGooglePlace = fetchedPlaces.find { place -> place.objectID == items[position].objectID }
        val placeImage = placesImages[items[position].objectID];

        var button = row!!.findViewById<Button>(elementId);
        var typeText = row!!.findViewById<TextView>(R.id.businessListTypeText)
        var icon = row!!.findViewById<ImageView>(R.id.businessListIcon);
        var closedOverlay = row!!.findViewById<ImageView>(R.id.businessListClosedOverlay)

        button!!.text = items[position].toString()
        typeText!!.text = fetchedGooglePlace?.type
        icon!!.visibility = View.VISIBLE

        if (fetchedGooglePlace != null)
        {
            closedOverlay.visibility = if (IsGooglePlaceCurrentlyOpen(
                    context,
                    fetchedGooglePlace!!.openingHours
                )
            ) View.GONE else View.VISIBLE
        }

        if (placeImage != null)
        {
            icon!!.setImageBitmap(placeImage)
        }

        return row!!
    }


    public fun setItems(newItems: Array<Business>)
    {
        items = newItems
    }

    public fun SetFetchedGooglePlaces(newFetchedPlaces: Array<FetchedGooglePlace>)
    {
        fetchedPlaces = newFetchedPlaces
    }

    public fun SetGooglePlacesImages(newPlacesImages: HashMap<String, Bitmap?>)
    {
        placesImages = newPlacesImages
    }
}