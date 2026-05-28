package com.example.app.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object Supabase {
    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://krhzmtvpsiwujlsbixev.supabase.co",
        supabaseKey = "sb_publishable_X2Ujv7OVrwwgVbn7OOAIJQ_sFBjlAeT"
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }
}
