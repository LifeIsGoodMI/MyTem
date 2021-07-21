package com.mytemcorporation.mytem.activities

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.mytemcorporation.mytem.*

class BusinessDetailsActivity : AppCompatActivity()
{
    private lateinit var googlePlacesManager: GooglePlacesManager
    private lateinit var mapsHandler: GoogleMapsHandler

    private lateinit var searchView: SearchView
    private lateinit var businessPhoto: ImageView
    private lateinit var businessRating: RatingBar
    private lateinit var businessTypeText: TextView
    private lateinit var businessAddressText: TextView
    private lateinit var businessOpeningHoursText: TextView
    private lateinit var googleMapsButton: ImageButton
    private var blockQueryTextChangeEvent = false

    private lateinit var fetchedGooglePlace: FetchedGooglePlace

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.business_details_screen)

        GetViews()
        SetupSearchView()

        fetchedGooglePlace = intent.getParcelableExtra(SingleBusinessResultParcelable)

        googlePlacesManager =
            GooglePlacesManager(this)
        mapsHandler = GoogleMapsHandler(
            this,
            googlePlacesManager,
            R.id.businessDetailsGoogleMap
        )

        googleMapsButton.setOnClickListener {
            mapsHandler.OpenMapsAppFromUri(fetchedGooglePlace)
        }

        SetBusinessDetails()
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
        searchView = findViewById(R.id.businessDetailsSearchView)
        businessPhoto = findViewById(R.id.businessDetailsPhoto)
        businessRating = findViewById(R.id.businessDetailsRating)
        businessTypeText = findViewById(R.id.businessDetailsType)
        businessAddressText = findViewById(R.id.businessDetailsAddress)
        businessOpeningHoursText = findViewById(R.id.businessDetailsOpeningHours)
        googleMapsButton = findViewById(R.id.businessDetailsMapsButton)
    }


    private fun SetBusinessDetails()
    {
        val address = fetchedGooglePlace.address
        val addressStrings = address.split(",")
        val formattedAddress = addressStrings[0] + ", " + addressStrings[1]
        val formattedOpeningHours =
            FormatOpeningHoursToday(
                this,
                fetchedGooglePlace.openingHours
            )

        businessRating.rating = fetchedGooglePlace.rating.toFloat()
        businessTypeText.text = fetchedGooglePlace.type
        businessAddressText.text = formattedAddress
        businessOpeningHoursText.text = formattedOpeningHours

        mapsHandler.ScheduleAction(mapsHandler.CenterMapOnMarkersAction, arrayOf(fetchedGooglePlace))

        googlePlacesManager.QueryPhotoFromPhotoMetaData(fetchedGooglePlace.photoMetadata!!, 1600, 1600){
            businessPhoto.setImageBitmap(it.result!!.bitmap)
        }
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

                val intent = Intent(this@BusinessDetailsActivity, ProductSearchActivity::class.java)
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

        val closeButton =
            GetSearchViewCloseButton(searchView)
        closeButton.setOnClickListener(object: View.OnClickListener
        {
            override fun onClick(v: View?)
            {
                val intent = Intent(this@BusinessDetailsActivity, MainScreenActivity::class.java)
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