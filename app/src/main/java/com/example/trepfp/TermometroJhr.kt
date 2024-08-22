package com.example.trepfp

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat


class TermometroJhr(context: Context?, attrs: AttributeSet?) : View(context, attrs) {


    //variables
    private var centroEscenarioX = 0f
    private var altoEscenario = 0f
    private var celdasAncho = 0f
    private var numeroCeldas = 3f
    private var centroEscenarioY = 0f
    private var ultimaCeldaAncho = 0f
    private var finalCentroY = 0f
    private var radioExterior = 0f
    private var radioInterior = 0f
    private var primerRadioExterior = 0f
    private var compensadorX = 0f
    private var compensadorY = 0f
    private var rango = 0f

    //GAME
    private var temperatura_maxima = 0f
    private var temperatura_anterior = 0f
    private var temperatura_minima = 0f

    private var temMaxima = 50f;
    private var temMinima = 0f


    //termometro lineas pintura
    private lateinit var interiorLineaPaint: Paint
    private lateinit var exteriorLineaPaint: Paint
    private lateinit var primerLineaExteriorPaint: Paint
    private lateinit var texto_ajustes: Paint
    private lateinit var primerArcoExteriorPaint:Paint
    private lateinit var interiorCirculoPaint: Paint
    private lateinit var exteriorCirculoPaint: Paint
    private lateinit var primerCiculoExterior: Paint
    private lateinit var textPaint:Paint



    //  private var color_termometro = Color.rgb(0, 0, 0)
    private var coroutineOk = false

    var timeAnimation = 3000f

    // Obtain a typed array of attributes
    var a = getContext().theme.obtainStyledAttributes(attrs,R.styleable.termometroJhr, 0, 0)
    private var color_termometro = a.getColor(R.styleable.termometroJhr_colorTermometro,Color.BLACK)
    private var color_temperatura = a.getColor(R.styleable.termometroJhr_colorTemperatura,Color.BLACK)
    private var textTemp = a.getString(R.styleable.termometroJhr_textTemp)
    private var offsetX = a.getFloat(R.styleable.termometroJhr_offsetX,0f)
    private var offsetY = a.getFloat(R.styleable.termometroJhr_offsetY,0f)
    private var textSizeT = a.getFloat(R.styleable.termometroJhr_textSizeTemp,5f)
    private var textColorTemp = a.getColor(R.styleable.termometroJhr_textColorTemp,Color.BLACK)
    private var textOn = a.getBoolean(R.styleable.termometroJhr_textOn,false)

    init {
        iniciar()
        a.recycle()
        if (textTemp==null){
            textTemp = "jhr"
        }

    }

    private fun iniciar() {
        textPaint = Paint(Paint.ANTI_ALIAS_FLAG)


        interiorCirculoPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        interiorCirculoPaint.setColor(color_temperatura)
        interiorCirculoPaint.setStyle(Paint.Style.FILL)
        interiorCirculoPaint.setStrokeWidth(12f)
        exteriorCirculoPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        exteriorCirculoPaint.setColor(Color.WHITE)
        exteriorCirculoPaint.setStyle(Paint.Style.FILL)
        exteriorCirculoPaint.setStrokeWidth(32f)
        primerCiculoExterior = Paint(Paint.ANTI_ALIAS_FLAG)
        primerCiculoExterior.setColor(color_termometro)
        primerCiculoExterior.setStyle(Paint.Style.FILL)
        primerCiculoExterior.setStrokeWidth(60f)
        primerArcoExteriorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        primerArcoExteriorPaint.setColor(color_termometro)
        primerArcoExteriorPaint.setStyle(Paint.Style.STROKE)
        primerArcoExteriorPaint.setStrokeWidth(30f)
        interiorLineaPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        interiorLineaPaint.setColor(color_termometro)
        interiorLineaPaint.setStyle(Paint.Style.FILL)
        interiorLineaPaint.setStrokeWidth(17f)
        exteriorLineaPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        exteriorLineaPaint.setColor(Color.WHITE)
        exteriorLineaPaint.setStyle(Paint.Style.FILL)
        primerLineaExteriorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        primerLineaExteriorPaint.setColor(color_termometro)
        primerLineaExteriorPaint.setStyle(Paint.Style.FILL)

        interiorCirculoPaint.setColor(color_temperatura)
        primerCiculoExterior.setColor(color_termometro)
        primerArcoExteriorPaint.setColor(color_termometro)
        interiorLineaPaint.setColor(color_termometro)
        primerLineaExteriorPaint.setColor(color_termometro)



    }



    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centroEscenarioX = width / 2.toFloat()
        altoEscenario = height.toFloat()
        celdasAncho = altoEscenario / numeroCeldas

        //Centro_primera_celda
        centroEscenarioY = celdasAncho / 2

        //mover a 3rd celda
        ultimaCeldaAncho = numeroCeldas*celdasAncho

        //centrado de ultima(3rd) celda
        finalCentroY = ultimaCeldaAncho - (celdasAncho / 2)

        //Radio Exterior 1/4 Celda Ancha
        //Radio Exterior 1/4 Celda Ancha
        radioExterior = (0.25f * celdasAncho) // width image
        radioInterior = (0.656f * radioExterior) // radio circle big center below
        primerRadioExterior = (1.344f * radioExterior)

        primerLineaExteriorPaint.setStrokeWidth(primerRadioExterior)
        exteriorLineaPaint.setStrokeWidth(primerRadioExterior / 2)
        primerArcoExteriorPaint.setStrokeWidth(primerRadioExterior / 4)
        interiorCirculoPaint.setStrokeWidth(radioInterior*0.5f)

        compensadorX = primerRadioExterior / 4
        compensadorX = compensadorX / 2

        //Captura el d/f btn primerLineaExterior and AnimacionInteriorLinea
        //Captura el d/f btn primerLineaExterior and AnimacionInteriorLinea
        compensadorY =
            centroEscenarioY + 0.875.toFloat() * radioExterior - (centroEscenarioY + radioInterior)
        compensadorY = compensadorY / 2
        temperatura_maxima = centroEscenarioY + (0.875f * radioInterior)
        temperatura_anterior = finalCentroY - (0.875f * radioInterior)
        temperatura_minima = finalCentroY - (0.875f * radioInterior)
        rango = temperatura_maxima - temperatura_minima

    }



    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        dibujarPrimerCirculoExterior(canvas) //circulo de abajo exterior
        dibujaExteriorCirculo(canvas) //
        dibujarInteriorCirculo(canvas)
        dibujarPrimerLineaExterior(canvas)
        dibujarLineaExterior(canvas) //linea a mover
        dibujarPrimerArcoExterior(canvas)
        if (textOn)dibujarText(canvas)
        interiorCirculoPaint.color = (color_temperatura)
        interiorCirculoPaint.color = (color_temperatura)
        primerCiculoExterior.color = (color_termometro)
        primerArcoExteriorPaint.color = (color_termometro)
        interiorLineaPaint.color = (color_termometro)
        primerLineaExteriorPaint.color = (color_termometro)

        if (!coroutineOk){
            dibujaranimacioninit(canvas)
        }else{
            dibujaranimacion(canvas)
        }

    }

    //Convierte coordenadas en y a grados
    private fun rulerInver3(fl: Float):Float{
        if (fl>= temperatura_maxima && fl<=temperatura_minima){
            //return (fl - temMinima) * (temperatura_maxima - temperatura_minima)/
            // (temMaxima - temMinima) + temperatura_minima
            return (fl-temperatura_minima)*(temMaxima-temMinima)/(temperatura_maxima-temperatura_minima)+temMinima
        }else{
            return 0f
        }
    }

    private fun dibujarText(canvas: Canvas) {
        textPaint.textSize = textSizeT
        textPaint.color = textColorTemp
        var grad = rulerInver3(temperatura_anterior)
        val decimalFormat = DecimalFormat("#.##")
        grad = decimalFormat.format(grad).toFloat()
        canvas.drawText("$grad$textTemp", centroEscenarioX + offsetX, (temperatura_maxima - temperatura_maxima * 0.28f) + offsetY, textPaint)
    }

    fun displayTemp(string: String){
        this.textTemp = string
    }


    private fun dibujaranimacion(canvas: Canvas) {
        val InicioY = finalCentroY - (0.875f * radioInterior)
        drawLine(canvas, InicioY, temperatura_anterior, interiorCirculoPaint)
        //Dibuja camino que luego sera llenado con el valor deseado
    }

    private fun dibujaranimacioninit(canvas: Canvas) {
        val InicioY = finalCentroY - (0.875f * radioInterior)
        drawLine(canvas, InicioY, temperatura_maxima+temperatura_maxima/2, interiorCirculoPaint)
        //Dibuja camino que luego sera llenado con el valor deseado
    }

    fun tempMax(tempMax :Float){
        temMaxima = tempMax
    }

    fun tempMin(tempMin:Float){
        temMinima = tempMin
    }


    fun tempSet(temp:Float) {

        var tempT = temp
        if (tempT > temMaxima) {
            tempT = temMaxima
        } else {
            if (tempT < temMinima) {
                tempT = temMinima
            }
        }

        tempT = reg3(tempT)

        val animator = ValueAnimator.ofFloat(temperatura_anterior,tempT)
        animator.setDuration(timeAnimation.toLong())

        if(!coroutineOk){
            coroutineOk = true
            animation(animator)
        }else{
            animator.cancel()
            animation(animator)
        }
    }

    private fun animation(animator: ValueAnimator){

        animator.addUpdateListener {
            temperatura_anterior = animator.animatedValue as Float
            invalidate()
        }
        animator.start()
    }


    private fun reg3(input:Float) : Float{
        if (input>= temMinima && input<=temMaxima){
            return (input - temMinima) * (temperatura_maxima - temperatura_minima)/
                    (temMaxima - temMinima) + temperatura_minima
        }else{
            return 0f
        }
    }

    //Dibuja linea intermedia blanca
    private fun dibujarLineaExterior(canvas: Canvas) {
        val InicioY = finalCentroY - (0.875f * radioExterior)
        val StopY = centroEscenarioY + (0.15f * radioExterior)//-> 0.875f

        drawLine(canvas, InicioY, StopY, exteriorLineaPaint)
        //Dibuja camino que luego sera llenado con el valor deseado
    }

    private fun drawLine(canvas: Canvas,inicioY: Float,stopY: Float,exteriorLineaPaint: Paint) {
        canvas.drawLine(centroEscenarioX, inicioY, centroEscenarioX, stopY, exteriorLineaPaint)
    }


    //Linea exterior desde arriba hasta donde init la circunferencia init
    private fun dibujarPrimerLineaExterior(canvas: Canvas) {
        val InicioY = finalCentroY - (0.875f * primerRadioExterior)
        val StopY = centroEscenarioY + (0.875f * radioExterior)

        drawLine(canvas, InicioY, StopY, primerLineaExteriorPaint)
        //linea que abarca Linea que encierra al valor que luego se llenara
    }

    //Arco parte superior
    private fun dibujarPrimerArcoExterior(canvas: Canvas) {
        val y  = centroEscenarioY - (0.875f * primerRadioExterior)
        val rectF = RectF(centroEscenarioX - primerRadioExterior / 2 + compensadorX,
            y + primerRadioExterior,centroEscenarioX + primerRadioExterior / 2 - compensadorX,
            y + 2 * primerRadioExterior + compensadorY
        )

        canvas.drawArc(rectF, -180f, 180f, false, primerArcoExteriorPaint)
    }


    //Circulo interior
    private fun dibujarInteriorCirculo(canvas: Canvas) {
        drawCircle(canvas, radioInterior, interiorCirculoPaint)
        //Dibuja Parte Exterior que rodea al circulo --->( () )<---
    }

    private fun dibujaExteriorCirculo(canvas: Canvas) {
        drawCircle(canvas, radioExterior.toFloat(), exteriorCirculoPaint)
        //Dibuja circulo Interior Encerrado ( -->(relleno blanco con filos color)<--- )
    }

    private fun dibujarPrimerCirculoExterior(canvas: Canvas) {
        drawCircle(canvas, primerRadioExterior, primerCiculoExterior)
        //Dibuja circulo Interior Encerrado ( -->(relleno)<--- )
    }

    private fun drawCircle(canvas: Canvas,Radio: Float,primerCiculoExterior: Paint) {
        canvas.drawCircle(centroEscenarioX, finalCentroY, Radio, primerCiculoExterior)
    }

    fun colorTermometro(color: Int){
        this.color_termometro = color
        invalidate()
    }

    fun colorTemperatura(color:Int){
        this.color_temperatura = color
        invalidate()
    }

    fun textOn(boolean: Boolean){
        this.textOn = boolean
        invalidate()
    }

}