package com.mytemcorporation.mytem.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mytemcorporation.mytem.R
import com.mytemcorporation.mytem.activities.BusinessDetailsActivity
import com.mytemcorporation.mytem.activities.ShoppingListBusinessResultsActivity
import kotlinx.coroutines.runBlocking

public class ShoppingListRecyclerViewAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>()
{
    override fun getItemCount(): Int
    {
        return 1
    }

    override fun getItemViewType(position: Int): Int
    {
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder
    {
        val inflater = parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        when (viewType)
        {
            0 ->
            {
                val view = inflater.inflate(R.layout.shopping_list_business_results_single_tab, parent, false)
                return ShoppingListSingleBusinessViewHolder(
                    context,
                    view
                )
            }

            else ->
            {
                val view = inflater.inflate(R.layout.shopping_list_business_results_single_tab, parent, false)
                return ShoppingListSingleBusinessViewHolder(
                    context,
                    view
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int)
    {
        when(position)
        {
            0 ->
            {
                val holder = holder as ShoppingListSingleBusinessViewHolder
                //holder.TryLoadSearchHistory()
            }
        }
    }
}


public class ShoppingListSingleBusinessViewHolder(private val context: Context, itemView: View) : RecyclerView.ViewHolder(itemView)
{
    private lateinit var countText: TextView
    private lateinit var list: ListView
    private lateinit var mapsButton: ImageButton

    private lateinit var adapter: BusinessAdapter

    init
    {
        Setup()
    }

    private fun Setup()
    {
        countText = itemView.findViewById(R.id.shoppingListResultsSingleTabCount)
        list = itemView.findViewById(R.id.shoppingListResultsSingleTabBusinessList)
        mapsButton = itemView.findViewById(R.id.shoppingListResultsSingleTabMapsButton)

        adapter = BusinessAdapter(
            context,
            emptyArray(),
            R.layout.business_list_element,
            R.id.businessListButton
        )
    }

    public fun SetupBusinessList()
    {
        val activity = context as ShoppingListBusinessResultsActivity

        list.adapter = adapter
        list.setOnItemClickListener(object : AdapterView.OnItemClickListener
        {
            override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
            {
                val business = adapter.getItem(position)

                val intent = Intent(context, BusinessDetailsActivity::class.java)
                //val fetchedGooglePlace = fetchedGooglePlaces.find { place -> place.objectID == business.objectID }
                //intent.putExtra(SingleBusinessResultParcelable, fetchedGooglePlace)

                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        })

        activity.runOnUiThread{
            runBlocking {
                //SearchBusinessesForProductAsync(productRefID)
            }
        }
    }
}