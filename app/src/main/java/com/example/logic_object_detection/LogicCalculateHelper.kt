import com.example.logic_object_detection.HelperResult
import java.lang.Math.pow
class LogicCalculateHelper(var calculateStr:String) {
    var variableStatus= mutableMapOf<String,Boolean>()
    var partOfLogicCalculateResult= mutableMapOf<String,Boolean>()
    var truthTableWithAnswer= mutableMapOf<String,Boolean>()
    fun calculate():HelperResult{
        val calculateList=calculateStr
            .replace("(","")
            .replace(")","")
            .split("(",")","and",
                "or","xor","nand","nor","not").map { it.trim() }
            .distinct()
            .filter { it.isNotEmpty() }.sortedDescending()
        val calculateNum:Int =(pow(2.0,calculateList.size.toDouble())).toInt()
        for(i in 0 until calculateNum){
            val binaryString=i.toString(2)
            val addZeroBinaryString=String
                .format("%${calculateList.size}s",binaryString).replace(" ","0")
            for(j in 0 until calculateList.size){
                val status=if(addZeroBinaryString[calculateList.size-j-1]=='0')false else true
                variableStatus.put(calculateList[j],status)
            }
            logicCalculate(calculateStr)
            logicCalculate(calculateStr)?.let { truthTableWithAnswer.put(addZeroBinaryString, it) }
        }
        println(truthTableWithAnswer)
        return HelperResult(truthTableWithAnswer,calculateList)
    }

    private fun logicCalculate(str:String):Boolean?{
        val partOfLogicFormula=findOutermostBrackets(str).toMutableList()
        partOfLogicFormula.add(str)
        var answer:Boolean?=null
        for(calculateFormula in partOfLogicFormula){
            if(!(calculateFormula.contains("(") && calculateFormula.contains(")"))){
                if (calculateFormula.contains(" and ")){
                    val splitStr=calculateFormula.split("and")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    val a=splitStr[0]
                    val b=splitStr[1]
                    val result=andCalculate(variableStatus.get(a)!!,variableStatus.get(b)!!)
                    partOfLogicCalculateResult.put("("+calculateFormula+")",result)
                    answer=result
                }
                else if(calculateFormula.contains("not ")){
                    val splitStr=calculateFormula.split("not")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    val a=splitStr[0]
                    val result=notCalculate(variableStatus.get(a)!!)
                    partOfLogicCalculateResult.put("("+calculateFormula+")",result)
                    answer=result
                }
                else if(calculateFormula.contains(" or ")){
                    val splitStr=calculateFormula.split("or")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    val a=splitStr[0]
                    val b=splitStr[1]
                    val result=orCalculate(variableStatus.get(a)!!,variableStatus.get(b)!!)
                    partOfLogicCalculateResult.put("("+calculateFormula+")",result)
                    answer=result
                }
                else if(calculateFormula.contains(" nand ")){
                    val splitStr=calculateFormula.split("nand")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    val a=splitStr[0]
                    val b=splitStr[1]
                    val result=nandCalculate(variableStatus.get(a)!!,variableStatus.get(b)!!)
                    partOfLogicCalculateResult.put("("+calculateFormula+")",result)
                    answer=result
                }
                else if(calculateFormula.contains(" nor ")){
                    val splitStr=calculateFormula.split("nor")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    val a=splitStr[0]
                    val b=splitStr[1]
                    val result=norCalculate(variableStatus.get(a)!!,variableStatus.get(b)!!)
                    partOfLogicCalculateResult.put("("+calculateFormula+")",result)
                    answer=result
                }
                else if(calculateFormula.contains(" xor ")){
                    val splitStr=calculateFormula.split("xor")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    val a=splitStr[0]
                    val b=splitStr[1]
                    val result=xorCalculate(variableStatus.get(a)!!,variableStatus.get(b)!!)
                    partOfLogicCalculateResult.put("("+calculateFormula+")",result)
                    answer=result
                }
            }
        }
        partOfLogicCalculateResult+=variableStatus
        partOfLogicFormula.sortBy { it.length }
        sortedResultMap()
        for (calculate in partOfLogicFormula){
            if(calculate.contains("(") || calculate.contains(")")) {
                var logic=calculate
                var count=0
                var first:Boolean?=null
                var second:Boolean?=null
                var calculatePair=Pair<Boolean?,Boolean?>(null,null)
                for (result in partOfLogicCalculateResult){
                    if(logic.contains(result.key)){
                        logic=logic.replace(result.key,"")
                        if(count==0)first=result.value
                        else if(count==1){
                            second=result.value
                        }
                        count++
                    }
                }
                logic=logic.replace(" ","")
                calculatePair= Pair(first, second)
                if (logic=="or"){
                    if(calculatePair.first!=null && calculatePair.second!=null){
                        answer=orCalculate(calculatePair.first!!, calculatePair.second!!)
                        partOfLogicCalculateResult.put("("+calculate+")",answer)
                    }
                }
                else if (logic=="not"){
                    if(calculatePair.first!=null){
                        answer=notCalculate(calculatePair.first!!)
                        partOfLogicCalculateResult.put("("+calculate+")",answer)
                    }
                }
                else if (logic=="nand"){
                    if(calculatePair.first!=null && calculatePair.second!=null){
                        answer=nandCalculate(calculatePair.first!!, calculatePair.second!!)
                        partOfLogicCalculateResult.put("("+calculate+")",answer)
                    }
                }
                else if (logic=="and"){
                    if(calculatePair.first!=null && calculatePair.second!=null){
                        answer=andCalculate(calculatePair.first!!, calculatePair.second!!)
                        partOfLogicCalculateResult.put("("+calculate+")",answer)
                    }
                }
                else if (logic=="nor"){
                    if(calculatePair.first!=null && calculatePair.second!=null){
                        answer=norCalculate(calculatePair.first!!, calculatePair.second!!)
                        partOfLogicCalculateResult.put("("+calculate+")",answer)
                    }
                }
                else if (logic=="xor"){
                    if(calculatePair.first!=null && calculatePair.second!=null){
                        answer=xorCalculate(calculatePair.first!!, calculatePair.second!!)
                        partOfLogicCalculateResult.put("("+calculate+")",answer)
                    }
                }
                sortedResultMap()
            }
        }
        return answer
    }
    private fun sortedResultMap() {
        partOfLogicCalculateResult=partOfLogicCalculateResult.entries
            .sortedByDescending { it.key.length }
            .associate { it.toPair() }
            .toMutableMap()
    }
    private fun findOutermostBrackets(s: String):List<String> {
        val stack = mutableListOf<Int>()
        for (i in s.indices) {
            when (s[i]) {
                '(' -> stack.add(i)
                ')' -> {
                    if (stack.isEmpty()) return listOf()
                    val startIndex = stack.removeAt(stack.lastIndex)
                    if (stack.isEmpty()){
                        val intRange=startIndex..i
                        val result=s.substring(intRange.start+1,intRange.last)
                        val non_check=s.substring(intRange.last+1)
                        return listOf(result)+findOutermostBrackets(result)+findOutermostBrackets(non_check)
                    }
                }
            }
        }
        return listOf()
    }
    fun andCalculate(a:Boolean,b:Boolean):Boolean=(a and b)

    fun nandCalculate(a:Boolean,b:Boolean):Boolean=!(a and b)

    fun orCalculate(a:Boolean,b:Boolean):Boolean=(a or b)

    fun xorCalculate(a:Boolean,b:Boolean):Boolean=(a xor b)

    fun norCalculate(a:Boolean,b:Boolean):Boolean=!(a or b)

    fun notCalculate(a:Boolean):Boolean=!a
}