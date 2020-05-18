package com.mytemcorporation.mytem

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import com.algolia.search.client.ClientSearch
import com.algolia.search.client.Index
import com.algolia.search.helper.deserialize
import com.algolia.search.model.APIKey
import com.algolia.search.model.ApplicationID
import com.algolia.search.model.IndexName
import com.algolia.search.model.search.Point
import com.algolia.search.model.search.Query
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
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

    private lateinit var searchClient: ClientSearch
    private lateinit var businessIndex: Index

    private var blockQueryTextChangeEvent = false
    private lateinit var lastFoundBusinesses: List<Business>
    private var fetchedGooglePlaces: MutableList<FetchedGooglePlace> = mutableListOf()
    private var fetchedGooglePlacesImages: HashMap<String, Bitmap?> = HashMap()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        setTheme(R.style.AppTheme_MyTem)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.business_results_screen)

        GetViews()

        googlePlacesManager = GooglePlacesManager(this)

        InitAlgolia()
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
        businessAdapter = BusinessAdapter(this, emptyArray(), R.layout.business_list_element, R.id.businessListButton)
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

    @SuppressLint("MissingPermission")
    private suspend fun SearchBusinessesForProductAsync(query: String)
    {
        // TODO: Notify user that location permissions are required.
        if (!CheckForLocationPermissions())
            return;

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val locationProvider = locationManager.allProviders[0]
        val location = locationManager.getLastKnownLocation(locationProvider)
        val locationPoint = Point(location.latitude.toFloat(), location.longitude.toFloat())

        val response = businessIndex.search(Query(query = query, aroundLatLng = locationPoint, getRankingInfo = true))
        val businesses = response.hits.deserialize(Business.serializer())

        businessAdapter.setItems(businesses.toTypedArray())
        businessList.adapter = null
        businessList.adapter = businessAdapter

        lastFoundBusinesses = businesses
        resultCountText.text = businesses.count().toString()

        FetchGooglePlacesFromBusinesses()
    }

    private fun FetchGooglePlacesFromBusinesses()
    {
        fetchedGooglePlaces.clear()

        for (business in lastFoundBusinesses)
        {
            val location = business._rankingInfo.matchedGeoLocation
            val position = LatLng(location.lat, location.lng)

            googlePlacesManager.QueryPlaces(business.business_name, position) {
                val place = it.result!!.autocompletePredictions[0]
                val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS,
                                    Place.Field.OPENING_HOURS, Place.Field.RATING, Place.Field.TYPES, Place.Field.PHOTO_METADATAS)

                googlePlacesManager.QueryDetailsFromPlace(place, fields){
                    val placeDetails = it.result!!.place
                    val photoMetadata = placeDetails.photoMetadatas!![0]

                    googlePlacesManager.QueryPhotoFromPhotoMetaData(photoMetadata) {
                        val placeType = GetPlaceType(this, placeDetails.types!!.toList())
                        val fetchedGooglePlace = FetchedGooglePlace(business.objectID, business.toString(), placeDetails.id!!, placeDetails.name!!, placeDetails.latLng!!,
                                                                    placeDetails.address!!, placeDetails.openingHours!!, placeDetails.rating!!, placeType,
                                                                    placeDetails.photoMetadatas!!.get(0))

                        fetchedGooglePlaces.add(fetchedGooglePlace)
                        fetchedGooglePlacesImages.put(business.objectID, it.result!!.bitmap)

                        businessAdapter.SetFetchedGooglePlaces(fetchedGooglePlaces.toTypedArray())
                        businessAdapter.SetGooglePlacesImages(fetchedGooglePlacesImages)
                        businessList.adapter = null
                        businessList.adapter = businessAdapter

                        if (fetchedGooglePlaces.count() == lastFoundBusinesses.count())
                        {
                            SortBusinessesByOpeningHours()
                            //FilterBusinessesByRadius(DefaultFilterRadius)
                        }
                    }
                }
            }
        }
    }

    private fun SortBusinessesByOpeningHours()
    {
        var sortedBusinesses = lastFoundBusinesses.toMutableList()
        var closedBusinesses = mutableListOf<Business>()
        for (place in fetchedGooglePlaces)
        {
            if (IsGooglePlaceCurrentlyOpen(this, place.openingHours))
                continue

            val business = lastFoundBusinesses.find { b -> b.objectID == place.objectID }
            sortedBusinesses.remove(business)
            closedBusinesses.add(business!!)
        }

        closedBusinesses.sortBy { it._rankingInfo.matchedGeoLocation.distance }
        sortedBusinesses.addAll(closedBusinesses)

        lastFoundBusinesses = sortedBusinesses
        businessAdapter.setItems(lastFoundBusinesses.toTypedArray())
        businessList.adapter = null
        businessList.adapter = businessAdapter
    }

    private fun FilterBusinessesByRadius(radiusInKm: Int)
    {
        val radiusInMeters = radiusInKm * 1000
        var businesses = lastFoundBusinesses.filter { it._rankingInfo.matchedGeoLocation.distance < radiusInMeters }
        businessAdapter.setItems(businesses.toTypedArray())
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

    //region Utility
    private fun CheckForLocationPermissions() : Boolean
    {
        val coarseLocationPermissionGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val fineLocationPermissionGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (coarseLocationPermissionGranted && fineLocationPermissionGranted)
            return true

        return false
    }

    private fun InitAlgolia()
    {
        val appID = ApplicationID(AlgoliaAppID)
        val apiKey = APIKey(AlgoliaAPIKey)
        val indexName = IndexName(AlgoliaBusinessIndex)

        searchClient = ClientSearch(appID, apiKey)
        businessIndex = searchClient.initIndex(indexName)
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
