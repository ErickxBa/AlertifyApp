package com.proyecto.alertify.app

class ActualizacionPerfilActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_actualizacion_perfil)

        val editButton = findViewById<ImageButton>(R.id.editButton) // Asegúrate de ponerle un ID
        editButton.setOnClickListener {
            val intent = Intent(this, ModificarPerfilActivity::class.java)
            startActivity(intent)
        }
    }
}
