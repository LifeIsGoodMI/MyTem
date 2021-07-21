package com.mytemcorporation.mytem.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import com.mytemcorporation.mytem.Product
import com.squareup.picasso.Picasso

class ProductAdapter(private var context: Context, private var items: Array<Product>,
                     private var layout: Int, private var elementIcon: Int, private var elementButton: Int) : BaseAdapter()
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
            row = LayoutInflater.from(context).inflate(layout, null, false);
        }

        var button = row?.findViewById<Button>(elementButton);
        var iconImage = row?.findViewById<ImageView>(elementIcon);

        button?.text = items[position].productname

        if (items[position].imageUrl != "")
        {
            Picasso.get().load(items[position].imageUrl).into(iconImage)
        }

        return row!!
    }


    public fun setItems(newItems: Array<Product>)
    {
        items = newItems
    }
}