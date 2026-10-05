import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import com.google.android.gms.maps.MapsInitializer.Renderer
import com.ishara.app.IsharaApp
import com.ishara.app.ui.theme.IsharaTheme

class MainActivity : ComponentActivity(), OnMapsSdkInitializedCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapsInitializer.initialize(
            applicationContext,
            Renderer.LEGACY,
            this
        )

        enableEdgeToEdge()

        setContent {
            IsharaTheme {
                IsharaApp()
            }
        }
    }

    override fun onMapsSdkInitialized(renderer: Renderer) {
        Log.d("MAP_TEST", "Renderer = $renderer")
    }
}