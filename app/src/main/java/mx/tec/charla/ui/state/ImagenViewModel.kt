package mx.tec.charla.ui.state

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.tec.charla.data.images.CompresorDeImagen
import mx.tec.charla.data.images.FotosTemporales
import mx.tec.charla.data.images.ImagenIlegible
import mx.tec.charla.data.local.SalaStore
import mx.tec.charla.data.remote.ChatConexion
import mx.tec.charla.data.remote.Orden
import mx.tec.charla.data.remote.SalaApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

data class ImagenUiState(val subiendo: Boolean = false, val error: String? = null)

//comprimirla, subirla al bucket, y decir su id :D
@HiltViewModel
class ImagenViewModel @Inject constructor(
    private val compresor: CompresorDeImagen,
    private val fotos: FotosTemporales,
    private val api: SalaApi,
    private val store: SalaStore,
    private val conexion: ChatConexion
) : ViewModel() {

    private val _ui = MutableStateFlow(ImagenUiState())
    val ui: StateFlow<ImagenUiState> = _ui.asStateFlow()

    //Para camara
    fun nuevaFoto(): Uri = fotos.nueva()

    fun enviar(uri: Uri) {
        if (_ui.value.subiendo) return
        _ui.value = ImagenUiState(subiendo = true)
        viewModelScope.launch {
            val error = try {
                val jpeg = compresor.comprimir(uri)
                val destino = store.destino.first()
                val parte = MultipartBody.Part.createFormData(
                    "archivo",
                    "foto.jpg",
                    jpeg.toRequestBody("image/jpeg".toMediaType())
                )
                val id = api.subirImagen("${destino.servidor}/imagenes", parte).id

                if (conexion.enviar(Orden.Enviar(texto = "", imagen = id))) null
                else "La foto se subió, pero se perdió la conexión con la sala"
            } catch (e: ImagenIlegible) {
                "No se pudo leer esa imagen"
            } catch (e: HttpException) {
                "La sala no aceptó la imagen (${e.code()})"
            } catch (e: IOException) {
                mensajeDe(e)
            }
            fotos.limpiar()
            _ui.value = ImagenUiState(error = error)
        }
    }

    fun errorVisto() = _ui.update { it.copy(error = null) }
}