package com.mytemcorporation.mytem

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import com.algolia.search.client.ClientSearch
import com.algolia.search.client.Index
import com.algolia.search.helper.deserialize
import com.algolia.search.model.APIKey
import com.algolia.search.model.ApplicationID
import com.algolia.search.model.IndexName
import com.algolia.search.model.search.Query
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json

class ProductSearchActivity : AppCompatActivity()
{
    private lateinit var searchView: SearchView
    private lateinit var productList: ListView

    private lateinit var productAdapter: ProductAdapter
    private lateinit var resultCountText: TextView

    private lateinit var searchClient: ClientSearch
    private lateinit var productIndex: Index

    override fun onCreate(savedInstanceState: Bundle?)
    {
        setTheme(R.style.AppTheme_MyTem)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_screen)

        GetViews()
        InitAlgolia()

        productList.setOnItemClickListener(object : AdapterView.OnItemClickListener
        {
            override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
            {
                HideAndroidKeyboard(searchView)
                OnProductItemClick(this@ProductSearchActivity, productAdapter, position)
            }
        })

        productAdapter = ProductAdapter(this, emptyArray(), R.layout.product_list_element, R.id.productListIcon, R.id.productListButton)
        productList.adapter = productAdapter

        SetupSearchView()
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
        searchView = findViewById<SearchView>(R.id.searchScreenSearchView)
        productList = findViewById(R.id.productList)
        resultCountText = findViewById(R.id.productResultCount);
    }


    private fun SetupSearchView()
    {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener
        {
            override fun onQueryTextChange(newText: String): Boolean
            {
                if (newText == "")
                {
                    productList.adapter = null
                    return false
                }

                runOnUiThread {
                    runBlocking {
                        UpdateProductListAsync(newText)
                    }
                }

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

                productList.adapter = null
            }
        })

        val closeButton = GetSearchViewCloseButton(searchView)
        closeButton.setOnClickListener(object: View.OnClickListener
        {
            override fun onClick(v: View?)
            {
                val intent = Intent(this@ProductSearchActivity, MainScreenActivity::class.java)
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                intent.putExtra(MainScreenSearchResetParcelable, true)

                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        })

        val query = intent.getStringExtra(SearchQueryParcelable)
        searchView.setQuery(query, false)
        searchView.requestFocus()
    }

    private suspend fun UpdateProductListAsync(query: String)
    {
        val response = productIndex.search(Query(query))
        val products = response.hits.deserialize(Product.serializer())

        productAdapter.setItems(products.toTypedArray())
        productList.adapter = null
        productList.adapter = productAdapter
        resultCountText.text = products.count().toString()
    }


    private fun InitAlgolia()
    {
        val appID = ApplicationID(AlgoliaAppID)
        val apiKey = APIKey(AlgoliaAPIKey)
        val indexName = IndexName(AlgoliaProductIndex)

        searchClient = ClientSearch(appID, apiKey)
        productIndex = searchClient.initIndex(indexName)
    }


    companion object {
        public fun OnProductItemClick(activity: AppCompatActivity, productAdapter: ProductAdapter, position: Int)
        {
            val product = productAdapter.getItem(position)
            val serializedProductJSON = Json.stringify(Product.serializer(), product)
            FileManager.WriteAdditiveToFile(activity, SearchHistoryFileName, serializedProductJSON, SearchHistoryFileMaxLineCount, { s -> s.contains(product.objectID) })

            val intent = Intent(activity, BusinessResultsActivity::class.java)
            intent.putExtra(SearchQueryParcelable, product.productname)
            intent.putExtra(BusinessResultsRefIDParcelable, product.refID)

            activity.startActivity(intent)
            activity.overridePendingTransition(0, 0)
        }
    }
}