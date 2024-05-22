package com.example.trepfp

import Utility.GetRealDataFromHexaOxxoDisplay
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.example.termometro_create.TermometroJhr
import com.example.trepfp.databinding.ActivityMain2Binding

class MainActivity2 : AppCompatActivity() {

    private lateinit var binding: ActivityMain2Binding
    var TermoCMO : TermometroJhr? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMain2Binding.inflate(layoutInflater)
        val view = binding.root

        TermoCMO = binding.termometro
        binding.button.setOnClickListener {
            TermoCmo(binding.textView3.text.toString())
            var check = if(binding.SwitchVaciado.isChecked) "0" else "1"
            modificarCardview( binding.textView3.text.toString().toInt(), binding.textViewCC.text.toString().toInt()  ,binding.llenado.text.toString().toInt() , 96,check)
        }



        setContentView(view)
    }

    private fun TermoCmo(temp: String) {

        TermoCMO?.tempMax(100F)
        TermoCMO?.tempMin(0F)
        TermoCMO?.timeAnimation = 1000F
        TermoCMO?.tempSet(temp.toFloat())

    }
    fun modificarCardview(poscionCB1 : Int, poscionCC1 : Int, NivelDeLLenado : Int, A1 : Int , CD : String){

        var ValorA1 = NivelDeLLenado //300

        val cardView =  binding.cardviewA1
        val cardviewTextos = binding.cardviewTextos

        var CD =    CD

        var aluraCardview =  cardView.height
        var poscionCB =(poscionCB1 * aluraCardview) / 100 // poscionCB1*2
        var poscionCC =(poscionCC1 * aluraCardview) / 100 // poscionCC1*2
        val NivelllenadoActual =(NivelDeLLenado * aluraCardview) / 100 // (A1.toDouble() / 100 * cardView.height).toInt() // cardView.height  * (NivelDeLLenado /100) .toInt()// A1 *  2
        binding.TextoLayoutCC.text = "CC Delta para  salir de  la alarma ${poscionCC1} %"
        binding.TextoLayoutCB.text = "CB Nivel para  activar  la alarma : ${poscionCB1} %"
        binding.TextoLayoutDif.text =  "Llenado actual  ${NivelDeLLenado} %"
        val linearLayoutCB =binding.linearLayoutCB
        val linearLayoutCC = binding.linearLayoutCC
        val linearLayoutDif = binding.linearLayoutDif
        Log.d(
            "debug---","---------------------------------------------> CD $CD    cardView ${cardView.height} NivelDeLLenado $NivelDeLLenado A1 $A1 NivelllenadoActual $NivelllenadoActual ")

        binding.TextoA1.text = "Altura máxima a medir: $A1"
//        if (porcentLLenado )

        if (CD == "0" /*direccionCD*/)
        {
            binding.imgArrowUp.isVisible  = true
            binding.imgArrowDown.isVisible  = false
        }
        else{
            binding.imgArrowUp.isVisible  = false
            binding.imgArrowDown.isVisible  = true
        }

        val linearLayoutCBTextos =binding.linearLayoutCBTextos
        val linearLayoutCCTextos = binding.linearLayoutCCTextos
        val linearLayoutDifTextos = binding.linearLayoutDifTextos



        val desplazamientoVerticalEnPixelesCB =   poscionCB //* 2
        val desplazamientoVerticalEnPixelesCC =   poscionCC

        Log.d(
            "debug---", "poscionCC $poscionCC desplazamientoVerticalEnPixelesCC $desplazamientoVerticalEnPixelesCC  \n " +
                    "poscionCB $poscionCB desplazamientoVerticalEnPixelesCB $desplazamientoVerticalEnPixelesCB" +
                    "\n poscionCB $poscionCB  nivel de llenado $NivelllenadoActual")
//        // Cambia el margen inferior del LinearLayout según el desplazamiento deseado
        val layoutParamsCB = linearLayoutCB.layoutParams as ConstraintLayout.LayoutParams
        layoutParamsCB.bottomMargin = desplazamientoVerticalEnPixelesCB  - 5
        linearLayoutCB.layoutParams = layoutParamsCB

//        val DeltatoAlarma =    if (CD == "1") {
//            desplazamientoVerticalEnPixelesCB - desplazamientoVerticalEnPixelesCC  - 5
//        }
//        else desplazamientoVerticalEnPixelesCB + desplazamientoVerticalEnPixelesCC  - 5

        Log.d("ValoresdELTA","desplazamientoVerticalEnPixelesCB $desplazamientoVerticalEnPixelesCB  \n  desplazamientoVerticalEnPixelesCC $desplazamientoVerticalEnPixelesCC")
        val DeltatoAlarma =    if (CD == "1") {
            desplazamientoVerticalEnPixelesCB + desplazamientoVerticalEnPixelesCC  - 5
        }
        else desplazamientoVerticalEnPixelesCB - desplazamientoVerticalEnPixelesCC  - 5


        val layoutParamsCC = linearLayoutCC.layoutParams as ConstraintLayout.LayoutParams
        layoutParamsCC.bottomMargin =DeltatoAlarma   // desplazamientoVerticalEnPixelesCB - desplazamientoVerticalEnPixelesCC  - 5
        linearLayoutCC.layoutParams = layoutParamsCC

//        ////////////////////////////////////////////////Textos
        val layoutParamsCBTextos = linearLayoutCBTextos.layoutParams as ConstraintLayout.LayoutParams
        layoutParamsCBTextos.bottomMargin = desplazamientoVerticalEnPixelesCB  - 10
        linearLayoutCBTextos.layoutParams = layoutParamsCBTextos

        val layoutParamsCCTextos = linearLayoutCCTextos.layoutParams as ConstraintLayout.LayoutParams
        layoutParamsCCTextos.bottomMargin = DeltatoAlarma // desplazamientoVerticalEnPixelesCB - desplazamientoVerticalEnPixelesCC  - 5
        linearLayoutCCTextos.layoutParams = layoutParamsCCTextos

        Log.d(
            "debug---",
            "aluraCardview $aluraCardview  NivelllenadoActual $NivelllenadoActual  NivelDeLLenado $NivelDeLLenado  desplazamientoVerticalEnPixelesCC $desplazamientoVerticalEnPixelesCC poscionCB $poscionCB    "
        )



        // Ajustar el tamaño y margen inferior de linearLayoutDif
        val layoutParamsDIF = linearLayoutDif.layoutParams as ConstraintLayout.LayoutParams
        layoutParamsDIF.height = NivelllenadoActual // Cambia según sea necesario
//        layoutParamsDIF.bottomMargin =
//            NivelllenadoActual - aluraCardview // Establece un margen negativo para que se extienda hasta el fondo
        linearLayoutDif.layoutParams = layoutParamsDIF


        val layoutParamsDIFTextos = linearLayoutDifTextos.layoutParams as ConstraintLayout.LayoutParams
        layoutParamsDIFTextos.height = NivelllenadoActual - 5 // Cambia según sea necesario
//        layoutParamsDIFTextos.bottomMargin =
//       NivelllenadoActual   -5 //- 100// Establece un margen negativo para que se extienda hasta el fondo
        linearLayoutDifTextos.layoutParams = layoutParamsDIFTextos
        cardviewTextos.isVisible = true
        cardView.isVisible = true

        if (CD == "0"){

            if (NivelllenadoActual >= desplazamientoVerticalEnPixelesCB) linearLayoutDif.setBackgroundResource(
                R.color.rojoOxxo
            )
            else if (NivelDeLLenado <= desplazamientoVerticalEnPixelesCC) linearLayoutDif.setBackgroundResource(
                R.color.azulCeleste
            )

        }
        else{

            if (NivelllenadoActual <= desplazamientoVerticalEnPixelesCB) linearLayoutDif.setBackgroundResource(
                R.color.rojoOxxo
            )
            else if (NivelDeLLenado >= desplazamientoVerticalEnPixelesCC) linearLayoutDif.setBackgroundResource(
                R.color.azulCeleste
            )


//            else   linearLayoutDif.setBackgroundResource(R.color.azulCeleste)
        }
//        if (NivelllenadoActual >poscionCB){
//            linearLayoutDif.setBackgroundResource(R.color.Azul)
//        }
//        else    {
//            linearLayoutDif.setBackgroundResource(R.color.rojoOxxo)
//
//        }
    }
}