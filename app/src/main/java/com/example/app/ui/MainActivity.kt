package com.example.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.app.AlertDialog
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.app.R
import com.example.app.modelos.Usuario
import com.example.app.supabase.Supabase
import com.example.app.utils.LoadingUtil
import com.example.app.ui.auth.LoginActivity
import com.example.app.ui.main.admin.AdminFragment
import com.example.app.ui.main.admin.UsuariosFragment
import com.example.app.ui.main.perfil.PerfilFragment
import com.example.app.ui.main.productos.CarritoFragment
import com.example.app.ui.main.productos.CatalogoFragment
import com.example.app.ui.main.productos.HomeFragment
import com.example.app.ui.main.productos.MisComprasFragment
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var bottomNav: BottomNavigationView
    private var userRole: String = "cliente"
    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        drawerLayout = findViewById(R.id.drawer_layout)
        bottomNav = findViewById(R.id.btnNav)
        val navView = findViewById<NavigationView>(R.id.navView)

        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        toggle.drawerArrowDrawable.color = ContextCompat.getColor(this, R.color.color_nombre_app)

        cargarFragment(HomeFragment())
        configurarBottomNav()

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    cargarFragment(HomeFragment())
                    bottomNav.selectedItemId = R.id.nav_home
                }
                R.id.navigation_productos -> {
                    cargarFragment(CatalogoFragment())
                    bottomNav.selectedItemId = R.id.navigation_productos
                }
                R.id.navigation_gestion -> {
                    cargarFragment(AdminFragment())
                    bottomNav.selectedItemId = R.id.navigation_gestion
                }
                R.id.navigation_usuarios -> {
                    cargarFragment(UsuariosFragment())
                    bottomNav.selectedItemId = R.id.navigation_usuarios
                }
                R.id.navigation_carrito -> {
                    cargarFragment(CarritoFragment())
                    bottomNav.selectedItemId = R.id.navigation_carrito
                }
                R.id.navigation_compras -> {
                    cargarFragment(MisComprasFragment())
                    bottomNav.selectedItemId = R.id.navigation_compras
                }
                R.id.navigation_perfil -> cargarFragment(PerfilFragment())
                R.id.navigation_logout -> logout()
            }
            drawerLayout.closeDrawers()
            true
        }

        cargarRolYConfigurarMenu(navView)
    }

    private fun configurarBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> cargarFragment(HomeFragment())
                R.id.navigation_productos -> cargarFragment(CatalogoFragment())
                R.id.navigation_carrito -> cargarFragment(CarritoFragment())
                R.id.navigation_compras -> cargarFragment(MisComprasFragment())
                R.id.navigation_gestion -> cargarFragment(AdminFragment())
                R.id.navigation_usuarios -> cargarFragment(UsuariosFragment())
            }
            true
        }
    }

    private fun cargarRolYConfigurarMenu(navView: NavigationView) {
        val userId = Supabase.client.auth.currentUserOrNull()?.id
        if (userId == null) {
            logout()
            return
        }

        loadingDialog = LoadingUtil.mostrarLoading(this, "Cargando...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val usuario = Supabase.client.from("usuarios")
                    .select { filter { eq("id", userId) } }
                    .decodeSingleOrNull<Usuario>()

                userRole = usuario?.rol ?: "cliente"

                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    val isAdmin = userRole == "admin"
                    val drawerMenu = navView.menu
                    drawerMenu.findItem(R.id.navigation_gestion)?.isVisible = isAdmin
                    drawerMenu.findItem(R.id.navigation_usuarios)?.isVisible = isAdmin
                    drawerMenu.findItem(R.id.navigation_carrito)?.isVisible = !isAdmin
                    drawerMenu.findItem(R.id.navigation_compras)?.isVisible = !isAdmin

                    bottomNav.menu.findItem(R.id.navigation_carrito)?.isVisible = !isAdmin
                    bottomNav.menu.findItem(R.id.navigation_compras)?.isVisible = !isAdmin
                    bottomNav.menu.findItem(R.id.navigation_gestion)?.isVisible = isAdmin
                    bottomNav.menu.findItem(R.id.navigation_usuarios)?.isVisible = isAdmin
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    Toast.makeText(this@MainActivity, "Error al cargar rol", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun logout() {
        loadingDialog = LoadingUtil.mostrarLoading(this, "Cerrando sesión...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Supabase.client.auth.signOut()
            } catch (_: Exception) {}
            withContext(Dispatchers.Main) {
                LoadingUtil.ocultarLoading(loadingDialog)
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
            }
        }
    }

    private fun cargarFragment(fragment: Fragment) {
        if (fragment is HomeFragment) {
            fragment.setOnNavigateListener { itemId ->
                when (itemId) {
                    R.id.navigation_productos -> {
                        cargarFragment(CatalogoFragment())
                        bottomNav.selectedItemId = R.id.navigation_productos
                    }
                    R.id.navigation_gestion -> {
                        cargarFragment(AdminFragment())
                        bottomNav.selectedItemId = R.id.navigation_gestion
                    }
                    R.id.navigation_usuarios -> {
                        cargarFragment(UsuariosFragment())
                        bottomNav.selectedItemId = R.id.navigation_usuarios
                    }
                }
            }
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
