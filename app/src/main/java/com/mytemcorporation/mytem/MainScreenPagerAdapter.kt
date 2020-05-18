package com.mytemcorporation.mytem

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.PagerAdapter
import kotlinx.serialization.json.Json

class MainScreenPagerAdapter(private val context: Context) : PagerAdapter()
{
    override fun isViewFromObject(view: View, `object`: Any): Boolean
    {
        return view == `object`
    }

    override fun getCount(): Int
    {
        return 2
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any)
    {
        container.removeView(`object` as View)
    }


    override fun instantiateItem(container: ViewGroup, position: Int): Any
    {
        val inflater = container.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        when (position)
        {
            0 -> return CreateSearchHistoryView(container, inflater)
            1 -> return CreateShoppingListView(container, inflater)

            else ->
            {
                return CreateSearchHistoryView(container, inflater)
            }
        }
    }

    override fun getItemPosition(`object`: Any): Int
    {
        return POSITION_NONE
    }


    //region Search History
    private fun CreateSearchHistoryView(container: ViewGroup, inflater: LayoutInflater) : View
    {
        val view = inflater.inflate(R.layout.search_history, container, false)
        container.addView(view, 0)

        val list = view.findViewById<ListView>(R.id.searchHistoryList)
        val adapter = ProductAdapter(context, emptyArray(), R.layout.product_list_element, R.id.productListIcon, R.id.productListButton)
        val emptyHistoryText = view.findViewById<TextView>(R.id.searchHistoryEmptyText)
        TryLoadSearchHistory(list, adapter, emptyHistoryText)

        return view
    }

    public fun TryLoadSearchHistory(list: ListView, adapter: ProductAdapter, emptyHistoryText: TextView)
    {
        val data = FileManager.TryReadFile(context, SearchHistoryFileName)

        if (data != null)
        {
            val products = mutableListOf<Product>()
            data.forEach {
                val product = Json.parse(Product.serializer(), it)
                products.add(product)
            }

            adapter.setItems(products.toTypedArray())
            list.adapter = null
            list.adapter = adapter

            emptyHistoryText.visibility = View.GONE

            list.setOnItemClickListener { adapterView, view, position, id ->
                ProductSearchActivity.OnProductItemClick(context as AppCompatActivity, adapter, position)
            }
        }
        else
        {
            list.adapter = null
            emptyHistoryText.visibility = View.VISIBLE
        }
    }
    //endregion

    //region Shopping List
    private fun CreateShoppingListView(container: ViewGroup, inflater: LayoutInflater) : View
    {
        val mainActivity = context as MainScreenActivity

        val view = inflater.inflate(R.layout.shopping_list_tab, container, false)
        container.addView(view, 0)

        val list = view.findViewById<ListView>(R.id.shoppingListTabShoppingList)
        val listCountText = view.findViewById<TextView>(R.id.shoppingListTabCountText)
        val noListsText = view.findViewById<TextView>(R.id.shoppingListTabEmptyText)
        val addButton = view.findViewById<Button>(R.id.shoppingListTabAddListButton)

        val adapter = ShoppingListAdapter(context, emptyArray())

        addButton.setOnClickListener {
            val intent = Intent(mainActivity, ShoppingListBuilderActivity::class.java)

            mainActivity.startActivity(intent)
            mainActivity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        TryLoadShoppingLists(list, adapter, listCountText, noListsText)

        return view
    }

    private fun TryLoadShoppingLists(list: ListView, adapter: ShoppingListAdapter, listCountText: TextView, noListsText: TextView)
    {
        val data = FileManager.TryReadFile(context, ShoppingListsFileName)

        if (data != null && data.count() > 0)
        {
            val shoppingLists = mutableListOf<ShoppingList>()
            data.forEach {
                val shoppingList = Json.parse(ShoppingList.serializer(), it)
                shoppingLists.add(shoppingList)
            }

            adapter.SetItems(shoppingLists.toTypedArray())
            list.adapter = null
            list.adapter = adapter

            noListsText.visibility = View.GONE
            val suffix = if (shoppingLists.count() == 1) context.getString(R.string.shopping_list_tab_count_single) else context.getString(R.string.shopping_list_tab_count)
            listCountText.text = shoppingLists.count().toString() + " " + suffix

            //TODO: Set OnItemClick listener
        }
        else
        {
            list.adapter = null
            noListsText.visibility = View.VISIBLE
            listCountText.text = ""
        }
    }
}