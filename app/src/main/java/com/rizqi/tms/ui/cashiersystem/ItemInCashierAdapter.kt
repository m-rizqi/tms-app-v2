package com.rizqi.tms.ui.cashiersystem

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rizqi.tms.R
import com.rizqi.tms.databinding.ItemCashierBinding
import com.rizqi.tms.model.ItemInCashier
import com.rizqi.tms.model.SubPrice
import com.rizqi.tms.model.SubPriceWithSpecialPrice
import com.rizqi.tms.utility.ThousandFormatter
import com.rizqi.tms.utility.hideKeyboard
import kotlin.math.ceil

class ItemInCashierAdapter : ListAdapter<ItemInCashier, ItemInCashierAdapter.ItemInCashierViewHolder>(DiffCallback){

    var onSubPriceChangedListener : ((ItemInCashier, SubPriceWithSpecialPrice, Int) -> ItemInCashier?)? = null
    var onIncrementQuantityListener : ((ItemInCashier, Int) -> ItemInCashier?)? = null
    var onDecrementQuantityListener : ((ItemInCashier, Int) -> ItemInCashier?)? = null

    companion object DiffCallback : DiffUtil.ItemCallback<ItemInCashier>(){
        override fun areItemsTheSame(oldItem: ItemInCashier, newItem: ItemInCashier): Boolean {
            return oldItem.itemId == newItem.itemId && oldItem.priceId == newItem.priceId && oldItem.subPriceId == newItem.subPriceId
        }

        override fun areContentsTheSame(oldItem: ItemInCashier, newItem: ItemInCashier): Boolean {
            return oldItem == newItem
        }

    }

    inner class ItemInCashierViewHolder(val binding : ItemCashierBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(itemInCashier: ItemInCashier, position: Int){
            val context = binding.root.context
            binding.apply {
                itemName = itemInCashier.itemWithPrices.item.name
                quantity = formatQuantity(itemInCashier.quantity)
                total = ThousandFormatter.format(itemInCashier.total)
                lItemCashierQuantity.tieAmount.setOnEditorActionListener { _, _, _ ->
                    hideKeyboard(binding.lItemCashierQuantity.tieAmount)
                    true
                }
                lItemCashierQuantity.btnMinus.setOnClickListener {
                    onDecrementQuantityListener?.invoke(itemInCashier, position)?.let { updatedItemCashier ->
                        binding.quantity = formatQuantity(updatedItemCashier.quantity)
                        binding.total = ThousandFormatter.format(updatedItemCashier.total)
                    }
                }
                lItemCashierQuantity.btnPlus.setOnClickListener {
                    onIncrementQuantityListener?.invoke(itemInCashier, position)?.let { updatedItemCashier ->
                        binding.quantity = formatQuantity(updatedItemCashier.quantity)
                        binding.total = ThousandFormatter.format(updatedItemCashier.total)
                    }
                }
            }

            val possibleSubPrice = mutableListOf<SubPriceWithSpecialPrice>()
            itemInCashier.itemWithPrices.prices.filter { priceAndSubPrice ->
                priceAndSubPrice.price.barcode == itemInCashier.barcode
            }.forEach { priceAndSubPrice ->
                possibleSubPrice.add(priceAndSubPrice.merchantSubPrice)
                possibleSubPrice.add(priceAndSubPrice.consumerSubPrice)
            }
            val priceArrayAdapter = ArrayAdapter<String>(
                context, R.layout.item_auto_complete, R.id.tv_item_auto_complete,
                possibleSubPrice.map { subPriceWithSpecialPrice ->
                    val subPrice = subPriceWithSpecialPrice.getSubPrice()
                    generatePriceHint(context, subPrice, itemInCashier)
                }
            )

            binding.actvItemCashierPrice.apply {
                setAdapter(priceArrayAdapter)
                hint = generatePriceHint(context, itemInCashier.usedSubPrice.getSubPrice(), itemInCashier)
//                hint = "${context.getString(R.string.rp_, ThousandFormatter.format(ceil(itemInCashier.usedSubPrice.getSubPrice().price).toLong()))}/${itemInCashier.itemWithPrices.prices.find { priceAndSubPrice -> priceAndSubPrice.price.id == itemInCashier.usedSubPrice.getSubPrice().priceId }?.price?.unitName} (${
//                    when(itemInCashier.usedSubPrice.getSubPrice()){
//                        is SubPrice.ConsumerSubPrice -> context.getString(R.string.consumer)
//                        is SubPrice.MerchantSubPrice -> context.getString(R.string.merchant)
//                    }
//                })"
                setHintTextColor(ResourcesCompat.getColor(context.resources, R.color.black_100, null))
                setOnItemClickListener { _, _, i, _ ->
                    val changedSubPrice = possibleSubPrice[i]
                    onSubPriceChangedListener?.invoke(itemInCashier, changedSubPrice, position)?.let { updatedItemCashier ->
                        binding.actvItemCashierPrice.apply {
                            setText("")
                            hint = generatePriceHint(context, updatedItemCashier.usedSubPrice.getSubPrice(), updatedItemCashier)
                            setHintTextColor(ResourcesCompat.getColor(context.resources, R.color.black_100, null))
                        }
                        binding.total = ThousandFormatter.format(updatedItemCashier.total)
                    }

                }
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemInCashierViewHolder {
        return ItemInCashierViewHolder(ItemCashierBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ItemInCashierViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    private fun formatQuantity(value : Double): String {
        var valueString = value.toString()
        val dotIndex = valueString.indexOf('.')
        if (dotIndex >= 0 && dotIndex <= valueString.lastIndex && valueString.substring(dotIndex).split("").all { it == "0" }){
            valueString = valueString.substring(0, dotIndex)
        }
        return valueString
    }

    private fun generatePriceHint(context: Context, subPrice : SubPrice, itemInCashier: ItemInCashier): String {
        return "${context.getString(R.string.rp_, ThousandFormatter.format(ceil(subPrice.price).toLong()))}/${itemInCashier.itemWithPrices.prices.find { priceAndSubPrice -> priceAndSubPrice.price.id == subPrice.priceId }?.price?.unitName} (${
            when(subPrice){
                is SubPrice.ConsumerSubPrice -> context.getString(R.string.consumer)
                is SubPrice.MerchantSubPrice -> context.getString(R.string.merchant)
            }
        })"
    }

}