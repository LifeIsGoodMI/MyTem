package com.mytemcorporation.mytem.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.mytemcorporation.mytem.Product
import com.mytemcorporation.mytem.R
import com.mytemcorporation.mytem.activities.ShoppingListBuilderActivity

class ShoppingListBuilderAdapter(private var context: Context, private var items: Array<Product>) : BaseAdapter()
{
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
        var row = convertView;
        if (row == null)
        {
            row = LayoutInflater.from(context).inflate(R.layout.shopping_list_builder_element, null, false);
        }

        var productNameText = row!!.findViewById<TextView>(R.id.shoppingListBuilderElementText)
        var removeButton = row!!.findViewById<ImageButton>(R.id.shoppingListBuilderElementRemoveButton)

        productNameText.text = items[position].productname
        removeButton.setOnClickListener {
            val activity = context as ShoppingListBuilderActivity
            activity.OnRemoveProductFromList(items[position])
        }

        return row!!
    }


    public fun SetItems(newItems: Array<Product>)
    {
        items = newItems
    }
}