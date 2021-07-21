package com.mytemcorporation.mytem.activities

import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import com.mytemcorporation.mytem.R

class ShoppingListBusinessResultsActivity : AppCompatActivity()
{
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?)
    {
        setTheme(R.style.AppTheme_MyTem)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.shopping_list_business_results_screen)

        GetViews()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean
    {
        return super.onTouchEvent(e)
    }

    override fun onBackPressed()
    {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun GetViews()
    {
        tabLayout = findViewById(R.id.shoppingListResultsTabLayout)
    }
}