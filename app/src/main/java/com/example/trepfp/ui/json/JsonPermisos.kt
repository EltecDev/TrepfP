package com.example.trepfp.ui.json

data class JsonPermisos(
    val aplicaciones: Aplicaciones
)
data class UsuariosX(
    val crear_usuarios: Boolean,
    val modificar_usuarios: Boolean
)
data class SeccionPlantillas(
    val descarga_bd: Boolean,
    val editar_plantilla: Boolean,
    val leer_parametros: Boolean,
    val solo_modbus: Boolean
)
data class Refris(
    val crear_refri: Boolean
)
data class Plantillas(
    val congelar_plantillas: Boolean,
    val plantillas_vigentes: Boolean
)
data class Clientes(
    val crear_clientes: Boolean
)
data class Aplicaciones(
    val Imbera_p: Boolean,
    val OxxoMonitor: Boolean,
    val TREFP_PC: Boolean,
    val Usuarios_Imbera: Boolean,
    val acciones_imberap: AccionesImberap,
    val acciones_trefp_pc: AccionesTrefpPc,
    val usuarios: UsuariosX
)
data class ArchConf(
    val eliminar_plantillas: Boolean,
    val grabar_plantillas: Boolean,
    val guardar_plantillas: Boolean,
    val importar_otro_control: Boolean,
    val importar_plantillas: Boolean,
    val obtener_plantilla: Boolean
)

data class AccionesImberap(
    val fw: Boolean,
    val jerarquia: String,
    val plantillas: Boolean,
    val seccion_plantillas: SeccionPlantillas,
    val tiempo_real: Boolean
)
data class AccionesTrefpPc(
    val arch_conf: ArchConf,
    val clientes: Clientes,
    val jerarquia: String,
    val plantillas: Plantillas,
    val produccion: Boolean,
    val refris: Refris,
    val seccion_arch_conf: Boolean,
    val seccion_clientes: Boolean,
    val seccion_plantillas: Boolean,
    val seccion_refris: Boolean,
    val seccion_usuarios: Boolean,
    val usuarios: UsuariosX
)
