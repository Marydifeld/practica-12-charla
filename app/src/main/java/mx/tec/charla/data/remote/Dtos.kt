package mx.tec.charla.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class AvisoDto(
    val id: Int,,
    val autor: String,
    val createdAt: String,
    /** La clave de la imagen en el servidor ("3f2a….jpg"), o null si el aviso no lleva. */
    val imagen: String? = null
)