package com.mytemcorporation.mytem.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.mytemcorporation.mytem.*
import com.mytemcorporation.mytem.adapters.MainScreenRecyclerViewAdapter


class MainScreenActivity : AppCompatActivity()
{
    private lateinit var searchView: SearchView
    private var blockQueryTextChangeEvent = false

    private lateinit var tabLayout: TabLayout
    private lateinit var searchHistoryAdapter: MainScreenRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_screen)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        //FileManager.DeleteFile(this, SearchHistoryFileName)

        GetViews()
        SetupSearchView()

        val viewPager2 = findViewById<ViewPager2>(R.id.mainScreenViewPager2)
        searchHistoryAdapter =
            MainScreenRecyclerViewAdapter(
                this
            )
        viewPager2.adapter = searchHistoryAdapter
        TabLayoutMediator(tabLayout, viewPager2, TabLayoutMediator.TabConfigurationStrategy { tab: TabLayout.Tab, i: Int ->
            when(i)
            {
                0 -> tab.setText(getString(R.string.search_history_tab))
                1 -> tab.setText(getString(R.string.shopping_list_tab))
            }
        }).attach()
    }

    override fun onStart()
    {
        super.onStart()

        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            val permissions = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
            requestPermissions(permissions, 0)
        }
    }

    override fun onResume()
    {
        super.onResume()
        UpdateMainScreenRecyclerViewAdapter()
    }

    override fun onRestart()
    {
        super.onRestart()
        UpdateMainScreenRecyclerViewAdapter()
    }

    // Intent is not updated when the activity is resumed / restarted.
    override fun onNewIntent(intent: Intent?)
    {
        super.onNewIntent(intent)

        // Clear the search view if the user has pressed the close button in one of the other activities.
        val searchResetted = intent!!.getBooleanExtra(MainScreenSearchResetParcelable, false)
        if (searchResetted)
            changeQueryTextBlocked("")
    }

    override fun onTouchEvent(e: MotionEvent): Boolean
    {
        searchView.clearFocus()
        return super.onTouchEvent(e)
    }

    private fun GetViews()
    {
        tabLayout = findViewById(R.id.mainScreenTabLayout)
        searchView = findViewById(R.id.mainScreenSearchView)
    }

    //region Search View
    private fun SetupSearchView()
    {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener
        {
            override fun onQueryTextChange(newText: String): Boolean
            {
                if (blockQueryTextChangeEvent || newText == "")
                    return false

                val intent = Intent(this@MainScreenActivity, ProductSearchActivity::class.java)
                intent.putExtra(SearchQueryParcelable, newText)

                startActivity(intent)
                overridePendingTransition(0, 0)

                return false
            }

            override fun onQueryTextSubmit(query: String): Boolean
            {
                searchView.clearFocus()
                return false
            }

        })

        val closeButton =
            GetSearchViewCloseButton(searchView)
        closeButton.setOnClickListener(object: View.OnClickListener
        {
            override fun onClick(v: View?)
            {
                changeQueryTextBlocked("")
            }
        })
    }

    private fun changeQueryTextBlocked(query: String)
    {
        blockQueryTextChangeEvent = true
        searchView.setQuery(query, false)
        blockQueryTextChangeEvent = false
    }
    //endregion

    public fun UpdateMainScreenRecyclerViewAdapter()
    {
        searchHistoryAdapter.notifyDataSetChanged()
    }
}