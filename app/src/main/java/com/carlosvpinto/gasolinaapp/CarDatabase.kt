package com.carlosvpinto.gasolinaapp

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// 1. LA TABLA (Entity)
@Entity(tableName = "cars")
data class CarEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val brand: String,
    val model: String,
    val capacityGallons: Double
)

// 2. LAS CONSULTAS (DAO)
@Dao
interface CarDao {
    // Obtener lista de marcas sin repetir y en orden alfabético
    @Query("SELECT DISTINCT brand FROM cars ORDER BY brand ASC")
    suspend fun getAllBrands(): List<String>

    // Obtener los modelos de una marca específica
    @Query("SELECT * FROM cars WHERE brand = :brandName ORDER BY model ASC")
    suspend fun getModelsByBrand(brandName: String): List<CarEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cars: List<CarEntity>)
}

// 3. LA BASE DE DATOS Y AUTO-RELLENADO
@Database(entities = [CarEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun carDao(): CarDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gasolina_app_database"
                )
                    // Esto rellena la base de datos la primera vez que se crea
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Llenamos la base de datos en segundo plano
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    val initialCars = listOf(
                        // Toyota
                        CarEntity(brand="Toyota", model="Corolla", capacityGallons=13.2),
                        CarEntity(brand="Toyota", model="Camry", capacityGallons=15.8),
                        CarEntity(brand="Toyota", model="RAV4", capacityGallons=14.5),
                        CarEntity(brand="Toyota", model="Tacoma", capacityGallons=21.1),
                        CarEntity(brand="Toyota", model="Highlander", capacityGallons=17.9),
                        // Ford
                        CarEntity(brand="Ford", model="F-150", capacityGallons=23.0),
                        CarEntity(brand="Ford", model="Escape", capacityGallons=14.8),
                        CarEntity(brand="Ford", model="Explorer", capacityGallons=18.6),
                        CarEntity(brand="Ford", model="Mustang", capacityGallons=16.0),
                        // Chevrolet
                        CarEntity(brand="Chevrolet", model="Silverado", capacityGallons=24.0),
                        CarEntity(brand="Chevrolet", model="Equinox", capacityGallons=14.9),
                        CarEntity(brand="Chevrolet", model="Malibu", capacityGallons=15.8),
                        CarEntity(brand="Chevrolet", model="Tahoe", capacityGallons=24.0),
                        // Honda
                        CarEntity(brand="Honda", model="Civic", capacityGallons=12.4),
                        CarEntity(brand="Honda", model="CR-V", capacityGallons=14.0),
                        CarEntity(brand="Honda", model="Accord", capacityGallons=14.8),
                        CarEntity(brand="Honda", model="Pilot", capacityGallons=19.5),
                        // Nissan
                        CarEntity(brand="Nissan", model="Sentra", capacityGallons=12.4),
                        CarEntity(brand="Nissan", model="Altima", capacityGallons=16.2),
                        CarEntity(brand="Nissan", model="Rogue", capacityGallons=14.5),
                        // Jeep
                        CarEntity(brand="Jeep", model="Wrangler", capacityGallons=17.5),
                        CarEntity(brand="Jeep", model="Grand Cherokee", capacityGallons=24.6),
                        // Hyundai
                        CarEntity(brand="Hyundai", model="Elantra", capacityGallons=12.4),
                        CarEntity(brand="Hyundai", model="Tucson", capacityGallons=14.3)
                    )
                    database.carDao().insertAll(initialCars)
                }
            }
        }
    }
}