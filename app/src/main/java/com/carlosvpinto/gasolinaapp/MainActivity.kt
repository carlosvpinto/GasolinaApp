package com.carlosvpinto.gasolinaapp

import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import android.media.AudioManager
import android.media.ToneGenerator
import android.animation.ObjectAnimator
import android.view.animation.DecelerateInterpolator
import com.carlosvpinto.gasolinaapp.adapters.GasStationAdapter


data class GasStation(
    val id: String, val name: String, val lat: Double, val lng: Double, val price: Double,
    var distanceKm: Double = 0.0 // <--- Agregamos esto
)
data class CarModel(val name: String, val capacityGallons: Double)

private val carDatabase = mapOf(
    "Toyota" to listOf(
        CarModel("Corolla", 13.2), CarModel("Camry", 15.8), CarModel("RAV4", 14.5), CarModel("Tacoma", 21.1)
    ),
    "Ford" to listOf(
        CarModel("F-150", 23.0), CarModel("Escape", 14.8), CarModel("Explorer", 18.6), CarModel("Mustang", 16.0)
    ),
    "Chevrolet" to listOf(
        CarModel("Silverado", 24.0), CarModel("Malibu", 15.8), CarModel("Equinox", 14.9), CarModel("Tahoe", 24.0)
    ),
    "Honda" to listOf(
        CarModel("Civic", 12.4), CarModel("CR-V", 14.0), CarModel("Accord", 14.8), CarModel("Pilot", 19.5)
    ),
    "Nissan" to listOf(
        CarModel("Sentra", 12.4), CarModel("Altima", 16.2), CarModel("Rogue", 14.5), CarModel("Frontier", 21.1)
    )
)


// NUEVA VARIABLE GLOBAL
private var precioEstacionSeleccionada: Double = 0.0

// Variable para generar el "Tick" del slider
private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

private var capacidadTanqueGalones = 14.0
private var nombreVehiculoGuardado = "Promedio USA" // <--- NUEVA VARIABLE

private lateinit var adapter: GasStationAdapter
private lateinit var gasolinerasOrdenadas: List<GasStation>

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<androidx.core.widget.NestedScrollView>
    private lateinit var drawerLayout: DrawerLayout

    // Variables Base
    private val userLat = 38.840000
    private val userLng = -76.950000
    private val precioPromedioGobierno = 3.60

    // Esta variable ahora cambiará según el auto que elijan
    private var capacidadTanqueGalones = 14.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. CARGAR DATOS GUARDADOS: Revisamos si el usuario ya había elegido un auto antes
        // 1. CARGAR DATOS GUARDADOS
        val sharedPref = getSharedPreferences("MisDatosApp", Context.MODE_PRIVATE)
        capacidadTanqueGalones = sharedPref.getFloat("capacidad_galones", 14.0f).toDouble()
        // Cargamos el nombre. Si no existe, usamos "Promedio USA"
        nombreVehiculoGuardado = sharedPref.getString("vehiculo_nombre", "Promedio USA") ?: "Promedio USA"

        // 2. CONFIGURAR EL MENÚ LATERAL (Hamburger Menu)
        drawerLayout = findViewById(R.id.drawer_layout)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)


        // 1. ENCUENTRAS EL PANEL EN EL XML
        val bottomSheet = findViewById<androidx.core.widget.NestedScrollView>(R.id.bottom_sheet_details)

        // 2. INICIALIZAS LA VARIABLE (¡Esta es la línea que seguramente falta o está debajo!)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        // 3. AHORA SÍ, LO OCULTAS
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN




        // Creamos el botón de las 3 rayitas y lo conectamos a la barra superior
        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, 0, 0)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()



        // Escuchar clics en el menú
        val navView = findViewById<NavigationView>(R.id.nav_view)
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_car -> mostrarDialogoVehiculo()
            }
            drawerLayout.closeDrawers() // Cierra el menú al elegir una opción
            true
        }

        // 3. CONFIGURAR MAPA Y PANEL (Lo mismo que ya tenías)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)
    }

    // --- NUEVAS FUNCIONES PARA EL VEHÍCULO ---

    private fun mostrarDialogoVehiculo() {
        // 1. Inflamos (Cargamos) el diseño XML
        val dialogView = layoutInflater.inflate(R.layout.dialog_select_car, null)

        val spinnerBrand = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerBrand)
        val spinnerModel = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerModel)

        val tvModelTitle = dialogView.findViewById<TextView>(R.id.tvModelTitle)
        val tvCustomHint = dialogView.findViewById<TextView>(R.id.tvCustomHint)
        val etCustomCapacity = dialogView.findViewById<EditText>(R.id.etCustomCapacity)

        // ACTUALIZAMOS LOS TEXTOS PROGRAMÁTICAMENTE (Por si dicen "Litros" en tu XML)
        tvCustomHint.text = "Ingresa la capacidad en Galones:"
        etCustomCapacity.hint = "Ejemplo: 14.5"

        // 2. Preparamos la lista de Marcas (+ la opción "Otro")
        val listaMarcas = carDatabase.keys.toMutableList()
        listaMarcas.add("Otra Marca / Personalizado ➕")

        // Conectamos la lista de marcas al primer Desplegable (Spinner)
        val brandAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listaMarcas)
        spinnerBrand.adapter = brandAdapter

        // 3. ¿Qué pasa cuando el usuario cambia de Marca?
        spinnerBrand.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val marcaSeleccionada = listaMarcas[position]

                if (marcaSeleccionada == "Otra Marca / Personalizado ➕") {
                    // Ocultar selector de modelo y mostrar caja de texto manual
                    spinnerModel.visibility = View.GONE
                    tvModelTitle.visibility = View.GONE
                    tvCustomHint.visibility = View.VISIBLE
                    etCustomCapacity.visibility = View.VISIBLE
                } else {
                    // Mostrar selector de modelo y ocultar manual
                    spinnerModel.visibility = View.VISIBLE
                    tvModelTitle.visibility = View.VISIBLE
                    tvCustomHint.visibility = View.GONE
                    etCustomCapacity.visibility = View.GONE

                    // Actualizar el segundo Spinner con los modelos de la marca elegida
                    val modelosDeLaMarca = carDatabase[marcaSeleccionada]!!
                    val nombresModelos = modelosDeLaMarca.map { it.name }

                    val modelAdapter = android.widget.ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, nombresModelos)
                    spinnerModel.adapter = modelAdapter
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // 4. Creamos y mostramos la ventana de Alerta
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Guardar Vehículo") { _, _ ->
                val marcaElegida = spinnerBrand.selectedItem.toString()

                if (marcaElegida == "Otra Marca / Personalizado ➕") {
                    val galones = etCustomCapacity.text.toString().toDoubleOrNull()
                    if (galones != null) {
                        // AQUÍ LE ENVIAMOS EL NOMBRE GENÉRICO
                        guardarCapacidad(galones, "Vehículo Personalizado")
                    } else {
                        Toast.makeText(this, "Ingresa un número válido.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val modeloElegido = spinnerModel.selectedItem.toString()
                    val carroEncontrado = carDatabase[marcaElegida]?.find { it.name == modeloElegido }

                    if (carroEncontrado != null) {
                        // AQUÍ LE ENVIAMOS LA MARCA Y MODELO (Ej: "Toyota Corolla")
                        guardarCapacidad(carroEncontrado.capacityGallons, "$marcaElegida $modeloElegido")
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // --- ACTUALIZAMOS TAMBIÉN EL MENSAJE DE ÉXITO ---
    private fun guardarCapacidad(galones: Double, nombreVehiculo: String) {
        capacidadTanqueGalones = galones
        nombreVehiculoGuardado = nombreVehiculo // Actualizamos la variable global

        val sharedPref = getSharedPreferences("MisDatosApp", Context.MODE_PRIVATE)
        sharedPref.edit()
            .putFloat("capacidad_galones", galones.toFloat())
            .putString("vehiculo_nombre", nombreVehiculo) // Guardamos el nombre
            .apply()

        Toast.makeText(this, "¡$nombreVehiculo guardado!", Toast.LENGTH_SHORT).show()
    }
    

    // --- FUNCIONES DEL MAPA (Iguales a las anteriores) ---

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // 1. APLICAR ESTILO OSCURO AL MAPA
        try {
            val success = mMap.setMapStyle(com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style))
            if (!success) android.util.Log.e("MapsActivity", "Falló al cargar el estilo del mapa.")
        } catch (e: Exception) {
            android.util.Log.e("MapsActivity", "No se puede encontrar el archivo de estilo.", e)
        }

        // 2. MARCADOR DEL USUARIO (TU CARRITO)
        val userLocation = LatLng(userLat, userLng)
        mMap.addMarker(
            MarkerOptions()
                .position(userLocation)
                .title("Tu ubicación actual")
                .icon(getResizedMapIcon(this, R.drawable.ic_my_car, 100, 70))
                .rotation(90f)
                .zIndex(1.0f)
        )
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 14f))

        // 3. NUESTRA LISTA DE GASOLINERAS
        val listaGasolineras = listOf(
            GasStation("1", "Exxon - Temple Hills", 38.847320, -76.953035, 3.15),
            GasStation("2", "Shell - Branch Ave", 38.835000, -76.948000, 3.50),
            GasStation("3", "Chevron - I-495", 38.842000, -76.965000, 3.80),
            GasStation("4", "Mobil - Allentown Rd", 38.830000, -76.955000, 3.10)
        )

        // 4. CALCULAR DISTANCIAS ANTES DE MOSTRARLAS EN LA LISTA
        for (station in listaGasolineras) {
            val results = FloatArray(1)
            Location.distanceBetween(userLat, userLng, station.lat, station.lng, results)
            station.distanceKm = results[0] / 1000.0
        }

        // 5. ORDENAR DE LA MÁS BARATA A LA MÁS CARA
        gasolinerasOrdenadas = listaGasolineras.sortedBy { it.price }

        // 6. DIBUJAR PINES EN EL MAPA CON SUS RANKINGS
        for ((index, station) in gasolinerasOrdenadas.withIndex()) {
            val rankingReal = index + 1
            val stationLocation = LatLng(station.lat, station.lng)

            // Lógica Mágica de Logos (Usando tus propios nombres exxon_2, shell_2, etc.)
            val nombreMinuscula = station.name.lowercase()
            val iconoSeleccionado = when {
                nombreMinuscula.contains("exxon") -> R.drawable.exxon_2
                nombreMinuscula.contains("shell") -> R.drawable.shell_2
                nombreMinuscula.contains("chevron") -> R.drawable.chevron_2
                nombreMinuscula.contains("mobil") -> R.drawable.mobil_2
                else -> R.drawable.gasolina_1
            }

            val marker = mMap.addMarker(
                MarkerOptions()
                    .position(stationLocation)
                    .title(station.name)
                    .icon(createMarkerWithRanking(this, iconoSeleccionado, rankingReal))
                    .zIndex(100f - rankingReal)
            )
            marker?.tag = station
        }

        // 7. CONFIGURAR LA LISTA (RECYCLERVIEW) INFERIOR
        val rvGasStations = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvGasStations)
        rvGasStations.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        adapter = GasStationAdapter(gasolinerasOrdenadas, capacidadTanqueGalones, precioPromedioGobierno) { clickedStation ->
            // ¿Qué pasa al tocar una tarjeta en la lista inferior?
            mostrarDetallesEstacion(clickedStation) // Actualiza el simulador arriba

            // Centramos el mapa en la estación tocada suavemente
            mMap.animateCamera(CameraUpdateFactory.newLatLng(LatLng(clickedStation.lat, clickedStation.lng)))
        }
        rvGasStations.adapter = adapter

        // 8. EVENTOS DE CLIC EN LOS PINES DEL MAPA
        mMap.setOnMarkerClickListener { clickedMarker ->
            if (clickedMarker.title == "Tu ubicación actual") return@setOnMarkerClickListener false

            val stationData = clickedMarker.tag as GasStation
            mostrarDetallesEstacion(stationData)

            // Sincronizar la lista para que se marque en verde la tarjeta correcta
            val posicionEnLista = gasolinerasOrdenadas.indexOf(stationData)
            if (posicionEnLista != -1) {
                val oldPos = adapter.selectedPosition
                adapter.selectedPosition = posicionEnLista
                adapter.notifyItemChanged(oldPos)
                adapter.notifyItemChanged(adapter.selectedPosition)
                // Hacer scroll automático en la lista hacia el elemento tocado
                rvGasStations.smoothScrollToPosition(adapter.selectedPosition)
            }
            true
        }

        // 9. EVENTOS DEL MAPA (Tocar un espacio vacío)
        mMap.setOnMapClickListener {
            // Bajamos el panel para dejar ver el mapa, pero no lo ocultamos del todo
            // para que el usuario siempre sepa que la lista sigue ahí abajo
            //bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            // Cuando toque el mapa, el panel se esconde por completo de nuevo
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        // 10. Seleccionar la más barata por defecto al abrir la app
//        if (gasolinerasOrdenadas.isNotEmpty()) {
//            mostrarDetallesEstacion(gasolinerasOrdenadas[0])
//        }
    }

    private fun mostrarDetallesEstacion(station: GasStation) {
        // 1. Guardamos el precio en la variable global para que el Slider lo pueda usar
        precioEstacionSeleccionada = station.price

        val results = FloatArray(1)
        Location.distanceBetween(userLat, userLng, station.lat, station.lng, results)

        // 2. Lógica Mágica de Logos
        val nombreMinuscula = station.name.lowercase()
        val iconoSeleccionado = when {
            nombreMinuscula.contains("exxon") -> R.drawable.exxon_2
            nombreMinuscula.contains("shell") -> R.drawable.shell_2
            nombreMinuscula.contains("chevron") -> R.drawable.chevron_2
            nombreMinuscula.contains("mobil") -> R.drawable.mobil_2
            else -> R.drawable.gasolina_1 // El genérico por si no es de marca
        }

        // 3. Actualizamos los Textos Estáticos
        findViewById<TextView>(R.id.tvCurrentVehicle).text = "Vehículo: $nombreVehiculoGuardado"
        findViewById<android.widget.ImageView>(R.id.ivStationLogo).setImageResource(iconoSeleccionado)
        findViewById<TextView>(R.id.tvStationName).text = station.name
        findViewById<TextView>(R.id.tvDistance).text = String.format("A %.1f km de ti", results[0] / 1000)
        findViewById<TextView>(R.id.tvPrice).text = "Precio: $${station.price} / Galón"

        // 4. CONFIGURAR EL SLIDER (SIMULADOR)
        // 4. CONFIGURAR EL SLIDER (SIMULADOR) CON LOS DATOS REALES DEL VEHÍCULO
        val sliderTank = findViewById<com.google.android.material.slider.Slider>(R.id.sliderTank)


        // ¡IMPORTANTE! Primero removemos los 'listeners' viejos para que no haya errores si tocas 2 gasolineras seguidas
        sliderTank.clearOnChangeListeners()

        // Ajustamos el tamaño del Slider a la capacidad del tanque del usuario guardado
        val maxCapacidad = capacidadTanqueGalones.toFloat()

        // Regla de Oro en Android: Siempre declarar 'valueTo' ANTES de asignar el 'value'
        sliderTank.valueTo = maxCapacidad
        sliderTank.value = maxCapacidad // Inicia con el tanque lleno

        // Llamamos a la función para que los números y el tanque 3D inicien sincronizados
        actualizarSimulador(capacidadTanqueGalones)

        // 5. ESCUCHAR LOS MOVIMIENTOS DEL SLIDER
        sliderTank.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                actualizarSimulador(value.toDouble())

                // Haptic Feedback (Vibración de engranaje)
                slider.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)

                // Sonido de "Tick" (El que configuramos con ToneGenerator)
                try {
                    toneGenerator.startTone(android.media.ToneGenerator.TONE_CDMA_PIP, 15)
                } catch (e: Exception) { }
            }
        }


        // 5. ESCUCHAR LOS MOVIMIENTOS DEL SLIDER
        sliderTank.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                actualizarSimulador(value.toDouble())

                // DOPAMINA 1: Vibración (Haptic Feedback)
                slider.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)

                // DOPAMINA 2: Sonido de "Tick" forzado
                // Usamos TONE_CDMA_PIP que es un "pip" cortito. El '15' son milisegundos de duración (súper rápido)
                try {
                    toneGenerator.startTone(ToneGenerator.TONE_SUP_RADIO_ACK, 40)
                } catch (e: Exception) {
                    // Prevenir cualquier error si el usuario mueve el slider demasiado rápido
                }
            }
        }

        // Botón Ir
        findViewById<Button>(R.id.btnNavigate).setOnClickListener {
            trazarRutaEnMapa(station.lat, station.lng)
        }

        // 5. Mostrar el panel a mitad de pantalla
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
    }


    // --- FUNCIÓN MATEMÁTICA CON ANIMACIÓN Y COLORES DINÁMICOS ---
    private fun actualizarSimulador(galonesALlenar: Double) {
        val ahorroPorGalon = precioPromedioGobierno - precioEstacionSeleccionada
        val ahorroTotal = ahorroPorGalon * galonesALlenar

        val tvSavings = findViewById<TextView>(R.id.tvSavings)
        val tvGalonesSimulador = findViewById<TextView>(R.id.tvGalonesSimulador)
        val pbTanqueGrafico = findViewById<android.widget.ProgressBar>(R.id.pbTanqueGrafico)
        val tvPorcentajeTanque = findViewById<TextView>(R.id.tvPorcentajeTanque)
        val sliderTank = findViewById<com.google.android.material.slider.Slider>(R.id.sliderTank)

        // 1. Calculamos el porcentaje
        val porcentaje = if (capacidadTanqueGalones > 0) {
            ((galonesALlenar / capacidadTanqueGalones) * 100).toInt()
        } else {
            0
        }

        // 2. LÓGICA DE COLORES DINÁMICOS (Basado en tu HTML)
        val colorNivel = when {
            porcentaje >= 60 -> android.graphics.Color.parseColor("#22C55E") // Verde (Full)
            porcentaje >= 25 -> android.graphics.Color.parseColor("#FBBF24") // Amarillo (Medio)
            else -> android.graphics.Color.parseColor("#EF4444") // Rojo (Bajo)
        }

        // Creamos un objeto de color para Android
        val colorStateList = android.content.res.ColorStateList.valueOf(colorNivel)

        // Aplicamos el color al Tanque
        pbTanqueGrafico.progressTintList = colorStateList

        // ¡EXTRA DE DISEÑO! Aplicamos el mismo color al Slider (Bolita y línea)
        sliderTank.thumbTintList = colorStateList
        sliderTank.trackActiveTintList = colorStateList

        // 3. Animamos el gráfico del tanque
        ObjectAnimator.ofInt(pbTanqueGrafico, "progress", pbTanqueGrafico.progress, porcentaje).apply {
            duration = 250
            interpolator = DecelerateInterpolator()
            start()
        }

        // 4. Actualizamos los textos descriptivos
        tvPorcentajeTanque.text = "$porcentaje%"
        tvGalonesSimulador.text = String.format("%.1f galones", galonesALlenar)

        // 5. Actualizamos el número gigante del Dinero
        if (ahorroTotal > 0) {
            tvSavings.text = String.format("+$%.2f", ahorroTotal)
            tvSavings.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.savings_green))
        } else if (ahorroTotal < 0) {
            tvSavings.text = String.format("-$%.2f", Math.abs(ahorroTotal))
            tvSavings.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.expensive_red))
        } else {
            tvSavings.text = "$0.00"
            tvSavings.setTextColor(android.graphics.Color.WHITE)
        }
    }


    private fun trazarRutaEnMapa(lat: Double, lng: Double) {
        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lng"))
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(packageManager) != null) startActivity(mapIntent)
        else startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")))
    }

    // --- FUNCIÓN PARA CONVERTIR Y REDIMENSIONAR LOGOS ---
    private fun getResizedMapIcon(context: android.content.Context, drawableResId: Int, widthSize: Int, heightSize: Int): com.google.android.gms.maps.model.BitmapDescriptor {
        val drawable = androidx.core.content.ContextCompat.getDrawable(context, drawableResId)!!

        // Creamos un lienzo vacío del tamaño exacto que queremos (ej. 100x100)
        val bitmap = android.graphics.Bitmap.createBitmap(widthSize, heightSize, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        // Dibujamos el logo adaptado a ese lienzo
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    // --- FUNCIÓN PARA DIBUJAR LOGO + NÚMERO DE RANKING ---
    // --- FUNCIÓN PARA DIBUJAR LOGO + NÚMERO DE RANKING ---
    private fun createMarkerWithRanking(context: android.content.Context, logoResId: Int, ranking: Int): com.google.android.gms.maps.model.BitmapDescriptor {
        val width = 120
        val height = 120
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        // Dibujar el Logo de la Gasolinera
        val logo = androidx.core.content.ContextCompat.getDrawable(context, logoResId)!!
        logo.setBounds(0, 20, 100, 120)
        logo.draw(canvas)

        // --- LA MAGIA DE LOS COLORES (Estilo Semáforo) ---
        val paintCircle = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        paintCircle.color = when (ranking) {
            1 -> android.graphics.Color.parseColor("#388E3C") // Verde (La más barata)
            2 -> android.graphics.Color.parseColor("#FFB300") // Amarillo / Ámbar (La segunda)
            else -> android.graphics.Color.parseColor("#E31837") // Rojo (La tercera y todas las demás)
        }

        // Dibujar el círculo
        val circleX = 90f
        val circleY = 30f
        val radius = 30f
        canvas.drawCircle(circleX, circleY, radius, paintCircle)

        // Configurar el Texto (El número de ranking)
        val paintText = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

        // TRUCO DE DISEÑO: Si es la #2 (Amarilla), usamos texto Negro. Si no, texto Blanco.
        paintText.color = if (ranking == 2) android.graphics.Color.BLACK else android.graphics.Color.WHITE

        paintText.textSize = 35f
        paintText.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        paintText.textAlign = android.graphics.Paint.Align.CENTER

        // Centrar y dibujar el texto
        val textBounds = android.graphics.Rect()
        val rankingText = ranking.toString()
        paintText.getTextBounds(rankingText, 0, rankingText.length, textBounds)
        val yOffset = textBounds.height() / 2f - textBounds.bottom

        canvas.drawText(rankingText, circleX, circleY + yOffset, paintText)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}