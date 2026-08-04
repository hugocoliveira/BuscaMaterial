package br.com.lit.busca.material.data.remote

import br.com.lit.busca.material.BuildConfig
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// ---------------------------------------------------------------------------
// Módulo de rede — constrói e expõe o singleton do ApiService.
// Credenciais injetadas via BuildConfig (valores vêm de local.properties,
// gitignored — nunca aparecem no código-fonte commitado).
// ---------------------------------------------------------------------------

/** URL base do servidor SAP. */
private const val BASE_URL = "http://vm77.4hub.cloud:57700/"

/** Credenciais SAP via BuildConfig — origem: local.properties (gitignored). */
private val SAP_USER get() = BuildConfig.SAP_USERNAME
private val SAP_PASS get() = BuildConfig.SAP_PASSWORD

/**
 * Objeto singleton que fornece a instância configurada de [ApiService].
 * Inicializado de forma lazy — criado apenas na primeira chamada.
 *
 * Configurações:
 * - Basic Auth em todas as requisições via interceptor OkHttp.
 * - Timeout de 30 segundos para conexão e leitura.
 * - Log de corpo completo em debug (pode ser reduzido em produção).
 */
object NetworkModule {

    /** Instância única do [ApiService], criada de forma lazy e thread-safe. */
    val apiService: ApiService by lazy { buildApiService() }

    /**
     * Constrói o [ApiService] com OkHttp + Basic Auth + Retrofit + Gson.
     * Chamado apenas uma vez pelo lazy delegate.
     */
    private fun buildApiService(): ApiService {
        // Interceptor de log — exibe corpo da requisição/resposta no Logcat
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Cliente OkHttp com Basic Auth e timeouts razoáveis para rede corporativa
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                // Adiciona o cabeçalho Authorization em toda requisição
                val requestComAuth = chain.request().newBuilder()
                    .header("Authorization", Credentials.basic(SAP_USER, SAP_PASS))
                    .build()
                chain.proceed(requestComAuth)
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        // Retrofit com conversor Gson para desserialização automática
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
