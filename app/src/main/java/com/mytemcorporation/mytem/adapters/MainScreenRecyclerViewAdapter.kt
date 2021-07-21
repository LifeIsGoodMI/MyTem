package com.mytemcorporation.mytem.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.mytemcorporation.mytem.*
import com.mytemcorporation.mytem.activities.MainScreenActivity
import com.mytemcorporation.mytem.activities.ProductSearchActivity
import com.mytemcorporation.mytem.activities.ShoppingListBuilderActivity
import kotlinx.serialization.json.Json


public class MainScreenRecyclerViewAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>()
{
    override fun getItemCount(): Int
    {
        return 2
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
                val view = inflater.inflate(R.layout.search_history_tab, parent, false)
                return SearchHistoryViewHolder(
                    context,
                    view
                )
            }

            1 ->
            {
                val view = inflater.inflate(R.layout.shopping_list_tab, parent, false)
                return ShoppingListViewHolder(
                    context,
                    view
                )
            }

            else ->
            {
                val view = inflater.inflate(R.layout.search_history_tab, parent, false)
                return SearchHistoryViewHolder(
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
                val holder = holder as SearchHistoryViewHolder
                holder.TryLoadSearchHistory()
            }

            1 ->
            {
                val holder = holder as ShoppingListViewHolder
                holder.TryLoadShoppingLists()
            }
        }
    }
}

public class SearchHistoryViewHolder(private val context: Context, itemView: View) : RecyclerView.ViewHolder(itemView)
{
    private lateinit var list: ListView
    private lateinit var emptyHistoryText: TextView

    private lateinit var adapter: ProductAdapter

    init
    {
        Setup()
    }

    private fun Setup()
    {
        list = itemView.findViewById(R.id.searchHistoryList)
        emptyHistoryText = itemView.findViewById(R.id.searchHistoryEmptyText)

        adapter = ProductAdapter(
            context,
            emptyArray(),
            R.layout.product_list_element,
            R.id.productListIcon,
            R.id.productListButton
        )
    }

    public fun TryLoadSearchHistory()
    {
        val data = FileManager.TryReadFile(
            context!!,
            SearchHistoryFileName
        )

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
}

public class ShoppingListViewHolder(private val context: Context, itemView: View) : RecyclerView.ViewHolder(itemView)
{
    private lateinit var list: ListView
    private lateinit var listCountText: TextView
    private lateinit var noListsText: TextView
    private lateinit var addButton: Button

    private lateinit var adapter: ShoppingListAdapter

    init
    {
        Setup()
    }

    private fun Setup()
    {
        list = itemView.findViewById(R.id.shoppingListTabShoppingList)
        listCountText = itemView.findViewById(R.id.shoppingListTabCountText)
        noListsText = itemView.findViewById(R.id.shoppingListTabEmptyText)
        addButton = itemView.findViewById(R.id.shoppingListTabAddListButton)

        adapter =
            ShoppingListAdapter(
                context,
                emptyArray()
            )

        addButton.setOnClickListener {
            val mainActivity = context as MainScreenActivity
            val intent = Intent(mainActivity, ShoppingListBuilderActivity::class.java)

            mainActivity.startActivity(intent)
            mainActivity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    public fun TryLoadShoppingLists()
    {
        val data = FileManager.TryReadFile(
            context,
            ShoppingListsFileName
        )

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
            val suffix = if (shoppingLists.count() == 1) context.getString(R.string.shopping_list_tab_count_single) else context.getString(
                R.string.shopping_list_tab_count
            )
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