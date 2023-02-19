package com.example.logic_object_detection

import LogicCalculateHelper
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.logic_object_detection.databinding.ActivityTruthTableBinding

class TruthTableActivity : AppCompatActivity() {
    private lateinit var binding:ActivityTruthTableBinding
    private var resultArrayList:ArrayList<CalculateResult> = arrayListOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title="ResultTable"
        binding=ActivityTruthTableBinding.inflate(layoutInflater)
        setContentView(binding.root)
        var variableStr=""
        val intent=intent
        val formula=intent.getStringExtra("formula")
        binding.textViewFormula.text="運算式："+formula
        val logicCalculateHelper= formula?.let { LogicCalculateHelper(it) }
        val helperResults=logicCalculateHelper?.calculate()
        val results=helperResults?.map
        val variables=helperResults?.variable?.sorted()
        Log.e("JAMES",results.toString())
        if(results!=null){
            for (result in results){
                resultArrayList.add(CalculateResult(result.key,result.value))
            }
        }
        if (variables != null) {
            for(variable in variables){
                variableStr+=" "+variable
            }
        }
        binding.textViewVariable.text=variableStr
        binding.recyclerViewTruthTable.apply {
            layoutManager=LinearLayoutManager(this@TruthTableActivity)
            adapter=MyTableAdapter(resultArrayList)
        }
    }
}