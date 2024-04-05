package mx.eltec.oxxomonitor.Sync


class DataSyncManagerFInal(private val query: String,
                           private val URL: String,
                           private val USER: String,
                           private val PASS: String,
                           private val Type: String
)  {


 //   val resultList = mutableListOf<Template>()
    suspend fun syncData(): List<Map<String, Any>> {
        // Obtén los datos desde la fuente remota (base de datos MySQL)
        val remoteData = obtenerDatosDesdeBaseDeDatosRemota()
        return remoteData

    }

    private suspend fun obtenerDatosDesdeBaseDeDatosRemota(): List<Map<String, Any>> {
        val dbHandler = DatabaseHandlerFinal(URL, USER, PASS)

        val results = dbHandler.executeQuery(query)
        return results


    }

 /*   private suspend fun mapToTemplate(results: List<Map<String, Any>>) {
        val resultList = mutableListOf<Template>()
        for (map in results) {
            val template = Template()
            // Asigna los valores desde el mapa a la instancia de Template
            template.name = map["Nombre_arch"].toString()
            template.plantilla = map["F"].toString()
            template.ble = map["n_ble"].toString()
            template.version = map["version"].toString()
            resultList.add(template)
        }

    }

    fun mapToTemplatePrincipal(map: Map<String, Any>): TemplatePrincipal {
        val templatePrincipal = TemplatePrincipal()
        templatePrincipal.name = map["nombre"] as? String ?: ""
        templatePrincipal.plantilla = map["plantilla"] as? String ?: ""
        templatePrincipal.n_ble = map["n_ble"] as? String ?: ""
        templatePrincipal.version = map["version"] as? String ?: ""
        return templatePrincipal
    }
    */


}