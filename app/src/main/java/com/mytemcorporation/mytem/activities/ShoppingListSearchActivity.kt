package com.mytemcorporation.mytem.activities

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.ListView
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.algolia.search.client.ClientSearch
import com.algolia.search.client.Index
import com.algolia.search.helper.deserialize
import com.algolia.search.model.ApplicationID
import com.algolia.search.model.IndexName
import com.algolia.search.model.search.Query
import com.mytemcorporation.mytem.*
import com.mytemcorporation.mytem.adapters.ShoppingListSearchAdapter
import kotlinx.coroutines.runBlocking

class ShoppingListSearchActivity : AppCompatActivity()
{
    private lateinit var returnButton: ImageButton
    private lateinit var searchView: SearchView
    private lateinit var resultCountText: TextView
    private lateinit var resultCountDescriptionText: TextView
    private lateinit var productList: ListView
    private lateinit var productAdapter: ShoppingListSearchAdapter

    private lateinit var searchClient: ClientSearch
    private lateinit var productIndex: Index

    private var products = arrayOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        setTheme(R.style.AppTheme_MyTem)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_screen)

        GetViews()
        InitAlgolia()

        productAdapter =
            ShoppingListSearchAdapter(
                this,
                emptyArray()
            )
        products = intent.getParcelableArrayListExtra<Product>(
            ShoppingListBuilderProductsParcelable
        ).toTypedArray()

        productAdapter.SetProducts(products.toMutableList())
        productList.adapter = productAdapter

        returnButton.setOnClickListener {
            ResumeToListBuilderActivity()
        }

        SetupSearchView()

        resultCountText.text = ""
        resultCountDescriptionText.text = ""
    }

    override fun onTouchEvent(e: MotionEvent): Boolean
    {
        searchView.clearFocus()
        return super.onTouchEvent(e)
    }

    override fun onBackPressed()
    {
        ResumeToListBuilderActivity()
    }

    private fun GetViews()
    {
        returnButton = findViewById(R.id.searchScreenReturnButton)
        searchView = findViewById(R.id.searchScreenSearchView)
        resultCountText = findViewById(R.id.productResultCount)
        resultCountDescriptionText = findViewById(R.id.productResultCountDescription)
        productList = findViewById(R.id.productList)
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
                    resultCountText.text = ""
                    resultCountDescriptionText.text = ""
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
    }

    private suspend fun UpdateProductListAsync(query: String)
    {
        val response = productIndex.search(Query(query))
        val products = response.hits.deserialize(Product.serializer())

        productAdapter.SetItems(products.toTypedArray())
        productList.adapter = null
        productList.adapter = productAdapter
        resultCountText.text = products.count().toString()
        resultCountDescriptionText.text = resources.getString(R.string.search_results_count)
    }

    public fun OnProductCollectionChanged(newProducts: Array<Product>)
    {
        products = newProducts
    }


    private fun ResumeToListBuilderActivity()
    {
        val intent = Intent(this, ShoppingListBuilderActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        intent.putParcelableArrayListExtra(ShoppingListSearchAlteredProductsParcelable, products.toCollection(ArrayList()))

        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun InitAlgolia()
    {
        val appID = ApplicationID(AlgoliaAppID)
        val apiKey = com.algolia.search.model.APIKey(AlgoliaAPIKey)
        val indexName = IndexName(AlgoliaProductIndex)

        searchClient = ClientSearch(appID, apiKey)
        productIndex = searchClient.initIndex(indexName)
    }
}