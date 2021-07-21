package com.mytemcorporation.mytem.activities

import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.*
import com.algolia.search.client.Index
import com.algolia.search.helper.deserialize
import com.algolia.search.model.search.Query
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mytemcorporation.mytem.*
import com.mytemcorporation.mytem.adapters.BusinessAdapter
import kotlinx.coroutines.runBlocking
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BusinessResultsActivity : AppCompatActivity()
{
    private lateinit var googlePlacesManager: GooglePlacesManager

    private lateinit var searchView: SearchView
    private lateinit var businessList: ListView
    private lateinit var businessAdapter: BusinessAdapter

    private lateinit var mapsButton: ImageButton
    private lateinit var resultCountText: TextView
    private lateinit var radiusSlider: SeekBar
    private lateinit var radiusText: TextView

    private lateinit var businessIndex: Index

    private var blockQueryTextChangeEvent = false
    private lateinit var lastFoundBusinesses: Array<Business>
    private var fetchedGooglePlaces: MutableList<FetchedGooglePlace> = mutableListOf()
    private var fetchedGooglePlacesImages: HashMap<String, Bitmap?> = HashMap()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        setTheme(R.style.AppTheme_MyTem)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.business_results_screen)

        GetViews()

        googlePlacesManager = GooglePlacesManager(this)

        businessIndex = InitAlgolia(AlgoliaBusinessIndex)
        SetupSearchView()
        SetupBusinessList()
        SetupMapsButton()

        searchView.clearFocus()
        HideAndroidKeyboard(searchView)

        //MapboxSearch()
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
        searchView = findViewById(R.id.buisnessResultsSearchView)
        businessList = findViewById(R.id.businessList)
        resultCountText = findViewById(R.id.buisnessResultCount)
        //radiusText = findViewById(R.id.businessListRadiusText)
        //radiusSlider = findViewById(R.id.businessListRadiusSlider)
        mapsButton = findViewById(R.id.businessResultsMapsButton)
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

                val intent = Intent(this@BusinessResultsActivity, ProductSearchActivity::class.java)
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

        searchView.setOnQueryTextFocusChangeListener(object : View.OnFocusChangeListener
        {
            override fun onFocusChange(v: View?, hasFocus: Boolean)
            {
                if (searchView.query != "")
                    return

                businessList.adapter = null
            }
        })

        val closeButton = GetSearchViewCloseButton(searchView)
        closeButton.setOnClickListener(object: View.OnClickListener
        {
            override fun onClick(v: View?)
            {
                val intent = Intent(this@BusinessResultsActivity, MainScreenActivity::class.java)
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

    //region Business List
    private fun SetupBusinessList()
    {
        businessAdapter = BusinessAdapter(
            this,
            emptyArray(),
            R.layout.business_list_element,
            R.id.businessListButton
        )
        businessList.adapter = businessAdapter
        businessList.setOnItemClickListener(object : AdapterView.OnItemClickListener
        {
            override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
            {
                HideAndroidKeyboard(searchView)

                val business = businessAdapter.getItem(position)

                val intent = Intent(this@BusinessResultsActivity, BusinessDetailsActivity::class.java)
                intent.putExtra(SearchQueryParcelable, searchView.query.toString())
                //intent.putExtra(SingleBusinessResultParcelable, business)
                val fetchedGooglePlace = fetchedGooglePlaces.find { place -> place.objectID == business.objectID }
                intent.putExtra(SingleBusinessResultParcelable, fetchedGooglePlace)

                startActivity(intent)
                overridePendingTransition(0, 0)
            }
        })

        val productRefID = intent.getStringExtra(BusinessResultsRefIDParcelable)
        runOnUiThread{
            runBlocking {
                SearchBusinessesForProductAsync(productRefID)
            }
        }

        /*
        radiusSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener
        {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean)
            {
                radiusText.text = (p0!!.progress + 1).toString() + "Km"
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}

            override fun onStopTrackingTouch(p0: SeekBar?)
            {
                FilterBusinessesByRadius(p0!!.progress + 1)
            }
        })*/
    }

    private suspend fun SearchBusinessesForProductAsync(query: String)
    {
        // TODO: Notify user that location permissions are required.
        if (!HasLocationPermissions(this))
            return;

        val locationPoint = GetDeviceLocation(this)
        val response = businessIndex.search(Query(query = query, aroundLatLng = locationPoint, getRankingInfo = true))
        lastFoundBusinesses = response.hits.deserialize(Business.serializer()).toTypedArray()

        UpdateBusinessList()
        resultCountText.text = lastFoundBusinesses.count().toString()

        FetchGooglePlacesFromBusinesses()
    }

    private fun FetchGooglePlacesFromBusinesses()
    {
        fetchedGooglePlaces.clear()

        for (business in lastFoundBusinesses)
        {
            val location = business._rankingInfo.matchedGeoLocation
            val position = LatLng(location.lat, location.lng)

            googlePlacesManager.FullGooglePlacesQuery(business, position) { place: Place, bitmap: Bitmap, fetchedGooglePlace: FetchedGooglePlace ->

                fetchedGooglePlaces.add(fetchedGooglePlace)
                fetchedGooglePlacesImages.put(business.objectID, bitmap)

                businessAdapter.SetFetchedGooglePlaces(fetchedGooglePlaces.toTypedArray())
                businessAdapter.SetGooglePlacesImages(fetchedGooglePlacesImages)
                businessList.adapter = null
                businessList.adapter = businessAdapter

                if (fetchedGooglePlaces.count() == lastFoundBusinesses.count())
                {
                    lastFoundBusinesses =
                        SortBusinessesByOpeningHours(
                            this,
                            lastFoundBusinesses,
                            fetchedGooglePlaces.toTypedArray()
                        )
                    UpdateBusinessList()
                    //FilterBusinessesByRadius(DefaultFilterRadius)
                }

            }
        }
    }

    private fun FilterBusinessesByRadius(radiusInKm: Int)
    {
        val radiusInMeters = radiusInKm * 1000
        var businesses = lastFoundBusinesses.filter { it._rankingInfo.matchedGeoLocation.distance < radiusInMeters }
        businessAdapter.setItems(businesses.toTypedArray())
        businessList.adapter = null
        businessList.adapter = businessAdapter
    }

    private fun UpdateBusinessList()
    {
        businessAdapter.setItems(lastFoundBusinesses)
        businessList.adapter = null
        businessList.adapter = businessAdapter
    }
    //endregion

    //region Maps button
    private fun SetupMapsButton()
    {
        mapsButton.setOnClickListener(object : View.OnClickListener
        {
            override fun onClick(v: View?)
            {
                val intent = Intent(this@BusinessResultsActivity, MapsActivity::class.java)
                intent.putExtra(SearchQueryParcelable, searchView.query.toString())
                intent.putParcelableArrayListExtra(BusinessResultsParcelable, fetchedGooglePlaces.toCollection(ArrayList()))

                startActivity(intent)
                overridePendingTransition(0, 0)
            }
        })
    }
    //endregion

    //region Mapbox EXPERIMENTAL
    private fun MapboxSearch()
    {
        val mapboxGeocoding = MapboxGeocoding.builder().accessToken(MapboxAccessToken).query("Lidl Köln Pesch").geocodingTypes("poi").build()
        mapboxGeocoding.enqueueCall(object : Callback<GeocodingResponse>
        {
            override fun onResponse(call: Call<GeocodingResponse>, response: Response<GeocodingResponse>)
            {
                val results = response.body()!!.features()
                for(place in results)
                {
                    println(place.placeName())
                    println(place.address())
                }
            }

            override fun onFailure(call: Call<GeocodingResponse>, throwable: Throwable)
            {
                throwable.printStackTrace()
            }
        })
    }
    //endregion
}
