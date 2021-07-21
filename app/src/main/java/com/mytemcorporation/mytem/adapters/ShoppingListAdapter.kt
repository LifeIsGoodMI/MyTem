package com.mytemcorporation.mytem.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.TextView
import com.mytemcorporation.mytem.*
import com.mytemcorporation.mytem.activities.MainScreenActivity
import com.mytemcorporation.mytem.activities.ShoppingListBuilderActivity

class ShoppingListAdapter(private var context: Context, private var items: Array<ShoppingList>) : BaseAdapter()
{
    override fun getItem(position: Int): ShoppingList
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
            row = LayoutInflater.from(context).inflate(R.layout.shopping_list_element, null, false);
        }

        var button = row!!.findViewById<TextView>(R.id.shoppingListButton)
        var editButton = row!!.findViewById<ImageButton>(R.id.shoppingListEditButton)
        var deleteButton = row!!.findViewById<ImageButton>(R.id.shoppingListDeleteButton)

        button.text = items[position].description
        editButton.setOnClickListener {
            val activity = context as MainScreenActivity
            val intent = Intent(activity, ShoppingListBuilderActivity::class.java)
            intent.putExtra(ShoppingListBuilderLoadedShoppingListParcelable, items[position])

            activity.startActivity(intent)
            activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        deleteButton.setOnClickListener {
            val success =
                FileManager.TryDeleteLineFromFile(
                    context,
                    ShoppingListsFileName,
                    items[position].description
                )
            if (success)
            {
                val activity = context as MainScreenActivity
                activity.UpdateMainScreenRecyclerViewAdapter()
            }
        }

        return row!!
    }


    public fun SetItems(newItems: Array<ShoppingList>)
    {
        items = newItems
    }
}