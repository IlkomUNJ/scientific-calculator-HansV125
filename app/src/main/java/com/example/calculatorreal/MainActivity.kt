package com.example.calculatorreal

import android.R
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calculatorreal.ui.theme.CalculatorRealTheme
import kotlin.collections.plusAssign
import kotlin.compareTo
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.text.compareTo
import kotlin.toString

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalculatorRealTheme {
                Surface (modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Numbers(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
    fun evaluateExpression(expression: String): String {
        val expression = expression
            .replace("−", "-")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("(-", "(0-")
            .replace(" ", "")
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < expression.length) {
            val char = expression[i]
            if (char.isDigit() || char == '.') {
                val start = i
                while (i < expression.length && (expression[i].isDigit() || expression[i] == '.')) {
                    i++
                }
                tokens.add(expression.substring(start, i))
            } else if (char in "+-*/%^") {
                if (char == '-' && (i == 0 || expression[i - 1] in "+-*/%(")) {
                    val start = i
                    i++
                    while (i < expression.length && (expression[i].isDigit() || expression[i] == '.')) {
                        i++
                    }
                    tokens.add(expression.substring(start, i))
                } else {
                    tokens.add(char.toString())
                    i++
                }
            } else if (char in "()") {
                tokens.add(char.toString())
                i++
            } else if (char.isLetter()) {
                val start = i
                while (i < expression.length && expression[i].isLetter()){
                    i++
                }
                val functionName = expression.substring(start, i)
                tokens.add(functionName)
            } else {
                throw IllegalArgumentException("Invalid character: $char")
            }
        }
        val numbers = ArrayDeque<Double>()
        val operators = ArrayDeque<String>()
        val precedence = mapOf("+" to 1, "-" to 1, "*" to 2, "/" to 2, "%" to 2, "^" to 3, "sin" to 4, "cos" to 4, "tan" to 4)
        val functions = setOf("sin", "cos", "tan")
        try {
            for (token in tokens) {
                when {
                    token.toDoubleOrNull() != null -> {
                        numbers.addLast(token.toDouble())
                    }
                    token == "(" -> operators.addLast(token)
                    token == ")" -> {
                        while (operators.isNotEmpty() && operators.last() != "(") {
                            performOperation(numbers, operators)
                        }
                        if (operators.isNotEmpty() && operators.last() == "("){
                            operators.removeLast()
                            if (operators.isNotEmpty() && operators.last() in functions){
                                performFunction(numbers, operators)
                            }
                        }
                    }
                    token in precedence.keys -> {
                        while (operators.isNotEmpty() &&
                            operators.last() != "(" &&
                            precedence.getOrDefault(operators.last(), 0) >= precedence.getOrDefault(token, 0)
                        ) {
                            performOperation(numbers, operators)
                        }
                        operators.addLast(token)
                    }
                }
            }

            while (operators.isNotEmpty()) {
                performOperation(numbers, operators)
            }
            if (numbers.size != 1) return "Error"
            val result = numbers.last()
            if (abs(result) < 1e-12) {
                return "0"
            }
            val formattedResult = "%.8f".format(result).trimEnd('0','.')
            return formattedResult
        } catch (e: Exception) {
            return "Error"
        }
    }
    private fun performFunction(numbers: ArrayDeque<Double>, operators: ArrayDeque<String>){
        val function = operators.removeLast()
        val operand = numbers.removeLast()
        val result = when (function) {
            "sin" -> sin(Math.toRadians(operand))
            "cos" -> cos(Math.toRadians(operand))
            "tan" -> {
                if ((operand % 180.0) == 90.0) {
                    throw ArithmeticException("Error")
                }
                tan(Math.toRadians(operand))
            }
            else -> throw IllegalArgumentException("Unknown function: $function")
        }
        numbers.addLast(result)
    }
    private fun performOperation(numbers: ArrayDeque<Double>, operators: ArrayDeque<String>) {
        val operator = operators.removeLast()
        val rightOperand = numbers.removeLast()
        if (operator == "%") {
            numbers.addLast(rightOperand / 100.0)
            return
        }
        val leftOperand = numbers.removeLast()
        val result = when (operator) {
            "+" -> leftOperand + rightOperand
            "-" -> leftOperand - rightOperand
            "*" -> leftOperand * rightOperand
            "/" -> {
                if (rightOperand == 0.0) throw ArithmeticException("Division by zero")
                leftOperand / rightOperand
            }
            "^" -> leftOperand.pow(rightOperand)
            else -> 0.0
        }
        numbers.addLast(result)
    }
    private fun factorial(n: Long): Long {
        if (n < 0) return -1
        if (n == 0L) return 1
        var result: Long = 1
        for (i in 1..n) {
            result *= i
        }
        return result
    }
    @Composable
    fun Numbers(modifier: Modifier) {
        var displayValue by remember { mutableStateOf("") }
        fun onButtonClick(buttonText: String) {
            val currentDisplay = displayValue
            val openParenthesesCount = currentDisplay.count { it == '(' }
            val closeParenthesesCount = currentDisplay.count { it == ')' }

            if (buttonText == "()"){
                if (currentDisplay.isEmpty() || currentDisplay.last().toString() in "+−×÷%("){
                    displayValue += "("
                }
                else if (openParenthesesCount > closeParenthesesCount && currentDisplay.last().isDigit()){
                    displayValue +=")"
                }
            } else {
                when (buttonText) {
                    "AC" -> displayValue = ""
                    "⌫" -> {
                        if (currentDisplay.length > 1) {
                            displayValue = currentDisplay.dropLast(1)
                        } else {
                            displayValue = ""
                        }
                    }
                    "√" -> {
                        val lastNumber = currentDisplay.toDoubleOrNull()
                        if (lastNumber != null &&  lastNumber >= 0){
                            val result = sqrt(lastNumber)
                            displayValue = result.toString()
                        } else {
                            displayValue = "Error"
                        }
                    }
                    "^" -> {
                        displayValue += "^"
                    }
                    "π" -> { displayValue += "3.14159265"}
                    "!" -> {
                        val lastNumber = currentDisplay.toDoubleOrNull()?.toInt()
                        if (lastNumber != null && lastNumber >= 0){
                            val result = factorial(lastNumber.toLong())
                            displayValue = result.toString()
                        } else {
                            displayValue = "Error"
                        }
                    }

                    "1/x" -> {
                        val lastNumber = currentDisplay.toDoubleOrNull()
                        if (lastNumber != null && lastNumber != 0.0){
                            val result = 1 / lastNumber
                            displayValue = result.toString()
                        } else {
                            displayValue = "Error"
                        }
                    }
                    "sin(" -> {
                        displayValue += "sin("
                    }
                    "cos(" -> {
                        displayValue += "cos("
                    }
                    "tan(" -> {
                        displayValue += "tan("
                    }

                    "inv" -> {

                    }
                    "e" -> { displayValue += "2.71828182"}
                    "ln" -> {
                        val lastNumber = currentDisplay.toDoubleOrNull()
                        if (lastNumber != null && lastNumber > 0) {
                            val result = ln(lastNumber)
                            displayValue = result.toString()
                        } else {
                            displayValue = "Error"
                        }

                    }
                    "log" -> {
                        val lastNumber = currentDisplay.toDoubleOrNull()
                        if (lastNumber != null && lastNumber > 0) {
                            val result = log10(lastNumber)
                            displayValue = result.toString()
                        } else {
                            displayValue = "Error"
                        }
                    }

                    "+", "-", "×", "÷", "%" -> {
                        val lastChar = currentDisplay.lastOrNull()
                        if (lastChar != null && lastChar.toString() in "+-×÷%") {
                            displayValue = currentDisplay.dropLast(1) + buttonText
                        } else {
                            displayValue += buttonText
                        }
                    }

                    "." -> {
                        val parts = currentDisplay.split("+", "-", "×", "÷", "%")
                        if (!parts.last().contains(".")) {
                            displayValue += buttonText
                        }
                    }
                    "=" -> {
                        displayValue = evaluateExpression(displayValue)
                    }

                    else -> {
                        if (currentDisplay == "0" && buttonText != ".") {
                            displayValue = buttonText
                        } else {
                            displayValue += buttonText
                        }
                    }
                }
            }
        }
        Box(modifier = modifier.fillMaxSize()) {
            Text(
                text = displayValue, modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(horizontal = 16.dp, vertical = 32.dp),
                textAlign = TextAlign.End, fontSize = 48.sp
            )
            Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)) {
                Row {
                    CalcButton(modifier = Modifier, "√") {onButtonClick("√")}
                    CalcButton(modifier = Modifier, "^") {onButtonClick("^")}
                    CalcButton(modifier = Modifier, "π") {onButtonClick("π")}
                    CalcButton(modifier = Modifier, "!") {onButtonClick("!")}
                }
                Row {
                    CalcButton(modifier = Modifier, "1/x") {onButtonClick("1/x")}
                    CalcButton(modifier = Modifier, "sin(") {onButtonClick("sin(")}
                    CalcButton(modifier = Modifier, "cos(") {onButtonClick("cos(")}
                    CalcButton(modifier = Modifier, "tan(") {onButtonClick("tan(")}
                }
                Row {
                    CalcButton(modifier = Modifier, "inv") { }
                    CalcButton(modifier = Modifier, "e") {onButtonClick("e")}
                    CalcButton(modifier = Modifier, "ln") {onButtonClick("ln")}
                    CalcButton(modifier = Modifier, "log") {onButtonClick("log")}
                }
                Row {
                    CalcButton(modifier = Modifier, "AC"){onButtonClick("AC")}
                    CalcButton(modifier = Modifier, "()"){onButtonClick("()")}
                    CalcButton(modifier = Modifier, "%"){onButtonClick("%")}
                    CalcButton(modifier = Modifier, "÷"){onButtonClick("÷")}
                }
                Row {
                    CalcButton(modifier = Modifier, "7") {displayValue += "7"}
                    CalcButton(modifier = Modifier, "8") {displayValue += "8"}
                    CalcButton(modifier = Modifier, "9") {displayValue += "9"}
                    CalcButton(modifier = Modifier, "×") {onButtonClick("×")}
                }
                Row {
                    CalcButton(modifier = Modifier, "4"){displayValue += "4"}
                    CalcButton(modifier = Modifier, "5"){displayValue += "5"}
                    CalcButton(modifier = Modifier, "6"){displayValue += "6"}
                    CalcButton(modifier = Modifier, "−"){onButtonClick("-")}
                }
                Row {
                    CalcButton(modifier = Modifier, "1"){displayValue += "1"}
                    CalcButton(modifier = Modifier, "2"){displayValue += "2"}
                    CalcButton(modifier = Modifier, "3"){displayValue += "3"}
                    CalcButton(modifier = Modifier, "+"){onButtonClick("+")}
                }
                Row {
                    CalcButton(modifier = Modifier, "0"){displayValue += "0"}
                    CalcButton(modifier = Modifier, "."){onButtonClick(".")}
                    CalcButton(modifier = Modifier, "⌫"){onButtonClick("⌫")}
                    CalcButton(modifier = Modifier, "="){onButtonClick("=")}
                }
            }
        }

    }
    @Composable
    fun CalcButton(modifier: Modifier = Modifier, textButton: String, buttonAction: () -> Unit) {
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        val buttonWidth = screenWidth / 4
        val buttonHeight = buttonWidth * 0.6f
        val fontSize = (buttonWidth.value * 0.3).sp
        Button(modifier = Modifier.size(width = buttonWidth, height = buttonHeight).padding(all = 1.dp), onClick = buttonAction, contentPadding = PaddingValues(all = 0.dp), shape = RoundedCornerShape(percent = 50)) {
            Text(text = textButton, fontSize = fontSize, textAlign = TextAlign.Center)
        }
    }
}
