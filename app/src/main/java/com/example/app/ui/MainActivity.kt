package com.example.app.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.app.R
import com.example.app.ui.main.admin.AdminFragment
import com.example.app.ui.main.admin.UsuariosFragment
import com.example.app.ui.main.perfil.PerfilFragment
import com.example.app.ui.main.productos.CarritoFragment
import com.example.app.ui.main.productos.CatalogoFragment
import com.example.app.ui.main.productos.HomeFragment
import com.example.app.ui.main.productos.MisComprasFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        drawerLayout = findViewById(R.id.drawer_layout)
        val BottomNav = findViewById<BottomNavigationView>(R.id.btnNav)
        val navView = findViewById<NavigationView>(R.id.navView)

        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        toggle.drawerArrowDrawable.color = resources.getColor(R.color.color_nombre_app)

        BottomNav.selectedItemId = R.id.nav_home

        BottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> cargarFragment(HomeFragment())
                R.id.navigation_productos -> cargarFragment(CatalogoFragment())
                R.id.navigation_carrito -> cargarFragment(CarritoFragment())
                R.id.navigation_compras -> cargarFragment(MisComprasFragment())
            }
//            navView.setCheckedItem(item.itemId)
            true
        }

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> cargarFragment(HomeFragment())
                R.id.navigation_perfil -> cargarFragment(PerfilFragment())
                R.id.navigation_productos -> cargarFragment(CatalogoFragment())
                R.id.navigation_admin -> cargarFragment(AdminFragment())
                R.id.navigation_usuarios -> cargarFragment(UsuariosFragment())
                R.id.navigation_logout -> finish()
            }
//            BottomNav.selectedItemId = item.itemId
            drawerLayout.closeDrawers()
            true
        }

    }

    private fun cargarFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}