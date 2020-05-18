package com.mytemcorporation.mytem

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.SearchView
import androidx.appcompat.widget.AppCompatImageView

class MapsActivity : AppCompatActivity()
{
    private lateinit var googlePlacesManager: GooglePlacesManager
    private lateinit var mapsHandler: GoogleMapsHandler

    private lateinit var searchView: SearchView
    private var blockQueryTextChangeEvent = false

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.maps_screen)

        GetViews()

        googlePlacesManager = GooglePlacesManager(this)
        mapsHandler = GoogleMapsHandler(this, googlePlacesManager, R.id.mapsScreenGoogleMap)

        SetupSearchView()

        val fetchedGooglePlaces: ArrayList<FetchedGooglePlace> = intent.getParcelableArrayListExtra(BusinessResultsParcelable)
        mapsHandler.ScheduleAction(mapsHandler.CenterMapOnMarkersAction, fetchedGooglePlaces.toTypedArray())
    }

    override fun onTouchEvent(e: MotionEvent): Boolean
    {
        searchView.clearFocus()
        return super.onTouchEvent(e)
    }

    override fun onBackPressed()
    {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun GetViews()
    {
        searchView = findViewById(R.id.mapsSearchView)
    }


    //region Search View
    private fun SetupSearchView()
    {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener
        {
            override fun onQueryTextChange(newText: String): Boolean
            {
                if (blockQueryTextChangeEvent)
                    return false;

                val intent = Intent(this@MapsActivity, ProductSearchActivity::class.java)
                intent.putExtra(SearchQueryParcelable, newText)

                startActivity(intent)
                overridePendingTransition(0, 0)

                return false
            }

            override fun onQueryTextSubmit(query: String): Boolean
            {
                searchView.clearFocus()
                HideAndroidKeyboard(searchView)

                return false
            }
        })

        val closeButton = GetSearchViewCloseButton(searchView)
        closeButton.setOnClickListener(object: View.OnClickListener
        {
            override fun onClick(v: View?)
            {
                val intent = Intent(this@MapsActivity, MainScreenActivity::class.java)
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                intent.putExtra(MainScreenSearchResetParcelable, true)

                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        })

        val query = intent.getStringExtra(SearchQueryParcelable)
        ChangeQueryTextBlocked(query)
    }

    private fun ChangeQueryTextBlocked(query: String)
    {
        var formattedQuery = query
        if (query[query.length - 1].isWhitespace())
        {
            formattedQuery = query.dropLast(1)
        }

        blockQueryTextChangeEvent = true;
        searchView.setQuery(formattedQuery, false);
        blockQueryTextChangeEvent = false;
    }
    //endregion
}
