package com.nuvio.app.core.network

object CineBackendConfig {
    /**
     * Domínio de Produção Oficial na Vercel (HTTPS) conectado ao Supabase e Mercado Pago.
     */
    const val VERCEL_API_URL = "https://cine-ultimate-app.vercel.app"

    const val SUPABASE_URL = "https://ivtpdmkyywoojbqnyusj.supabase.co"

    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Iml2dHBkbWt5eXdvb2picW55dXNqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODIwODUwNDgsImV4cCI6MjA5NzY2MTA0OH0.KZZ0MrtFp7bAzb2cBq0ysOXnQEUtvcGy0mof6hErAvg"

    val baseUrl: String
        get() = VERCEL_API_URL.trim().removeSuffix("/").ifBlank { "http://10.0.2.2:3333" }

    /**
     * URL direta do checkout de pagamento mobile
     */
    val checkoutUrl: String
        get() = baseUrl
}
