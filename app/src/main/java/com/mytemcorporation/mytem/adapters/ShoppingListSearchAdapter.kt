package com.mytemcorporation.mytem.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import com.mytemcorporation.mytem.Product
import com.mytemcorporation.mytem.R
import com.mytemcorporation.mytem.activities.ShoppingListSearchActivity
import com.squareup.picasso.Picasso

class ShoppingListSearchAdapter (private var context: Context, private var items: Array<Product>) : BaseAdapter()
{
    private lateinit var products: MutableList<Product>

    override fun getItem(position: Int): Product
    {
        return items[position]
    }

    override fun getItemId(position: Int): Long
    {
        return position.toLong()
    }

    override fun getCount(): Int
    {
        return items.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View
    {
        val activity = context as ShoppingListSearchActivity
        var row = convertView;
        if (row == null)
        {
            row = LayoutInflater.from(context).inflate(R.layout.shopping_list_search_element, null, false);
        }

        val product = items[position]
        var button = row?.findViewById<Button>(R.id.shoppingListSearchButton);
        var iconImage = row?.findViewById<ImageView>(R.id.shoppingListSearchIcon);
        var checkmarkImage = row?.findViewById<ImageView>(R.id.shoppingListSearchCheckmark)

        button!!.text = items[position].productname
        checkmarkImage!!.visibility = if (DoesListContainProduct(product)) View.VISIBLE else View.GONE

        if (items[position].imageUrl != "")
        {
            Picasso.get().load(items[position].imageUrl).into(iconImage)
        }

        button!!.setOnClickListener {
            if (!DoesListContainProduct(product))
            {
                products.add(product)
                checkmarkImage!!.visibility = View.VISIBLE
            }
            else
            {
                products.remove(product)
                checkmarkImage!!.visibility = View.GONE
            }

            activity.OnProductCollectionChanged(products.toTypedArray())
        }

        return row!!
    }

    private fun DoesListContainProduct(product: Product) : Boolean
    {
        return products.find { p -> p.productname == product.productname } != null
    }


    public fun SetItems(newItems: Array<Product>)
    {
        items = newItems
    }

    public fun SetProducts(newProducts: MutableList<Product>)
    {
        products = newProducts
    }
}