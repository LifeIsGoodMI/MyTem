package com.mytemcorporation.mytem.activities

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageButton
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.mytemcorporation.mytem.*
import com.mytemcorporation.mytem.adapters.ShoppingListBuilderAdapter
import kotlinx.serialization.json.Json

class ShoppingListBuilderActivity : AppCompatActivity()
{
    private lateinit var listDescription: TextInputLayout
    private lateinit var addProductButton: Button
    private lateinit var returnButton: ImageButton
    private lateinit var productList: ListView

    private var products = mutableListOf<Product>()
    private lateinit var productAdapter: ShoppingListBuilderAdapter

    override fun onCreate(savedInstanceState: Bundle?)
    {
        setTheme(R.style.AppTheme_MyTem)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.shopping_list_builder_screen)

        GetViews()

        productAdapter =
            ShoppingListBuilderAdapter(
                this,
                emptyArray()
            )

        addProductButton.setOnClickListener {
            val intent = Intent(this, ShoppingListSearchActivity::class.java)
            intent.putParcelableArrayListExtra(ShoppingListBuilderProductsParcelable, products.toCollection(ArrayList()))

            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        TryReadLoadedList()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean
    {
        listDescription.clearFocus()
        HideAndroidKeyboard(listDescription)
        return super.onTouchEvent(e)
    }

    override fun onBackPressed()
    {
        WriteShoppingListToStorage()

        val intent = Intent(this, MainScreenActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)

        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    // Intent is not updated when the activity is resumed / restarted.
    override fun onNewIntent(intent: Intent?)
    {
        super.onNewIntent(intent)

        val alteredProducts: ArrayList<Product> = intent!!.getParcelableArrayListExtra(
            ShoppingListSearchAlteredProductsParcelable
        )
        products = alteredProducts
        UpdateProductList()
    }

    private fun GetViews()
    {
        listDescription = findViewById(R.id.shoppingListBuilderInputLayout)
        addProductButton = findViewById(R.id.shoppingListBuilderAddProductButton)
        returnButton = findViewById(R.id.shoppingListBuilderReturnButton)
        productList = findViewById(R.id.shoppingListBuilderList)
    }

    // If the edit button has been pressed on a shopping list, the specific shopping list will be loaded in.
    private fun TryReadLoadedList()
    {
        val loadedShoppingList = intent.getParcelableExtra<ShoppingList>(
            ShoppingListBuilderLoadedShoppingListParcelable
        )
        if (loadedShoppingList == null)
            return

        listDescription.editText!!.setText(loadedShoppingList.description)
        products = loadedShoppingList.products.toMutableList()
        UpdateProductList()
    }


    private fun UpdateProductList()
    {
        productAdapter.SetItems(products.toTypedArray())
        productList.adapter = null
        productList.adapter = productAdapter
    }

    public fun OnRemoveProductFromList(product: Product)
    {
        products.remove(product)
        UpdateProductList()
    }


    private fun WriteShoppingListToStorage()
    {
        if (listDescription.editText!!.text.toString() == "")
            return

        val shoppingList = ShoppingList(
            listDescription.editText!!.text.toString(),
            products.toTypedArray()
        )
        val shoppingListJson = Json.stringify(ShoppingList.serializer(), shoppingList)
        FileManager.WriteAdditiveToFile(
            this,
            ShoppingListsFileName,
            shoppingListJson,
            ShoppingListFileMaxLineCount,
            { s -> s.contains(shoppingList.description) })
    }
}