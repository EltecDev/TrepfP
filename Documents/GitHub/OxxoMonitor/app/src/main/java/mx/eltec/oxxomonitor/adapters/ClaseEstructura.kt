package mx.eltec.oxxomonitor.adapters

import android.util.Log

data class RangoSTR(val clave: String, val inicio: Int, val final: Int, var dato: String = "")

class ClaseEstructura {
    companion object {
        val rangos = listOf(
            RangoSTR("DS1", 0, 2),
            RangoSTR("T0", 2, 6),
            RangoSTR("T1", 6, 10),
            RangoSTR("T2", 10, 14),
            RangoSTR("T3", 14, 18),
            RangoSTR("T4", 18, 22),
            RangoSTR("T5", 22, 26),
            RangoSTR("T6", 26, 30),
            RangoSTR("T7", 30, 34),
            RangoSTR("T8", 34, 38),
            RangoSTR("T9", 38, 42),
            RangoSTR("TA", 42, 46),
            RangoSTR("TB", 46, 50),
            RangoSTR("TC", 50, 54),
            RangoSTR("TD", 54, 58),
            RangoSTR("TE", 58, 62),
            RangoSTR("TF", 62, 66),
            RangoSTR("A0", 66, 70),
            RangoSTR("A1", 70, 74),
            RangoSTR("A2", 74, 78),
            RangoSTR("A3", 78, 82),
            RangoSTR("A4", 82, 86),
            RangoSTR("A5", 86, 90),
            RangoSTR("A6", 90, 94),
            RangoSTR("A7", 94, 98),
            RangoSTR("A8", 98, 102),
            RangoSTR("A9", 102, 106),
            RangoSTR("AA", 106, 110),
            RangoSTR("AB", 110, 114),
            RangoSTR("AC", 114, 118),
            RangoSTR("AD", 118, 122),
            RangoSTR("AE", 122, 126),
            RangoSTR("AF", 126, 130),
            RangoSTR("DS2", 130, 132),
            RangoSTR("L0", 132, 134),
            RangoSTR("L1", 134, 136),
            RangoSTR("L2", 136, 138),
            RangoSTR("L3", 138, 140),
            RangoSTR("L4", 140, 142),
            RangoSTR("L5", 142, 144),
            RangoSTR("L6", 144, 146),
            RangoSTR("L7", 146, 148),
            RangoSTR("L8", 148, 150),
            RangoSTR("L9", 150, 152),
            RangoSTR("LA", 152, 154),
            RangoSTR("LB", 154, 156),
            RangoSTR("LC", 156, 158),
            RangoSTR("LD", 158, 160),
            RangoSTR("LE", 160, 162),
            RangoSTR("LF", 162, 164),
            RangoSTR("C0", 164, 166),
            RangoSTR("C1", 166, 168),
            RangoSTR("C2", 168, 170),
            RangoSTR("C3", 170, 172),
            RangoSTR("C4", 172, 174),
            RangoSTR("C5", 174, 176),
            RangoSTR("C6", 176, 178),
            RangoSTR("C7", 178, 180),
            RangoSTR("C8", 180, 182),
            RangoSTR("C9", 182, 184),
            RangoSTR("CA", 184, 186),
            RangoSTR("CB", 186, 188),
            RangoSTR("CC", 188, 190),
            RangoSTR("CD", 190, 192),
            RangoSTR("CE", 192, 194),
            RangoSTR("CF", 194, 196),
            RangoSTR("F0", 196, 198),
            RangoSTR("F1", 198, 200),
            RangoSTR("F2", 200, 202),
            RangoSTR("F3", 202, 204),
            RangoSTR("F4", 204, 206),
            RangoSTR("F5", 206, 208),
            RangoSTR("F6", 208, 210),
            RangoSTR("F7", 210, 212),
            RangoSTR("F8", 212, 214),
            RangoSTR("F9", 214, 216),
            RangoSTR("FA", 216, 218),
            RangoSTR("FB", 218, 220),
            RangoSTR("FC", 220, 222),
            RangoSTR("FD", 222, 224),
            RangoSTR("FE", 224, 226),
            RangoSTR("FF", 226, 228),
            RangoSTR("D0", 228, 230),
            RangoSTR("D1", 230, 232),
            RangoSTR("D2", 232, 234),
            RangoSTR("D3", 234, 236),
            RangoSTR("D4", 236, 238),
            RangoSTR("D5", 238, 240),
            RangoSTR("D6", 240, 242),
            RangoSTR("D7", 242, 244),
            RangoSTR("E0", 244, 246),
            RangoSTR("E1", 246, 248),
            RangoSTR("E2", 248, 250),
            RangoSTR("E3", 250, 254),
            RangoSTR("DS3", 254, 256)
        )
        /*fun obtenerDatos(): List<String> {
            return rangos.map { it.dato }
        }*/
        fun actualizarCampoEstructura(clave: String, nuevoDato: String) {
            val valorTo = rangos.find { it.clave == clave }
            Log.d("debugDatos","valorTo ${valorTo!!.clave} ${valorTo.dato} nuevoDato $nuevoDato")
            valorTo?.dato = nuevoDato
        }
        fun llenarDatos(dato: String){
            var cadena = dato  //sp!!.getString("plantillaDefaultAA", "")
            for (rango in rangos) {
                val clave = rango.clave
                val inicio = rango.inicio
                val final = rango.final
                val subcadena = cadena!!.substring(inicio, final)
                val valorTo = rangos.find { it.clave == clave }
                valorTo!!.dato = subcadena
                Log.d("updateInterfazFromPlantilla", "clave $clave subcadena $subcadena")
            }
        }
        fun obtenerDatos(): String {
            return rangos.joinToString(separator = "") { it.dato }
        }
        fun returnDato (campo : String):String{
            val valorTo = rangos.find { it.clave == campo }
           return  valorTo!!.dato
        }
    }


}