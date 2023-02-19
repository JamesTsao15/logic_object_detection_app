package com.example.logic_object_detection

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MyTableAdapter(val data:ArrayList<CalculateResult>):RecyclerView.Adapter<MyTableAdapter.ViewHolder>() {
    lateinit var context:Context
    class ViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        val tv_variableStatus:TextView
        val tv_result:TextView
        init{
            tv_variableStatus=itemView.findViewById<TextView>(R.id.textView_variableStatus)
            tv_result=itemView.findViewById<TextView>(R.id.textView_result)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context=parent.context
        val view=LayoutInflater.from(context)
            .inflate(R.layout.recycleview_logic_calculate_result,parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result:String=if(data[position].result)"1" else "0"
        var statStr=""
        for(i in data[position].variableStatus.indices){
            statStr+=" "+data[position].variableStatus[i]
        }
        holder.tv_variableStatus.text=statStr
        holder.tv_result.text=result
    }

    override fun getItemCount(): Int =data.size
}