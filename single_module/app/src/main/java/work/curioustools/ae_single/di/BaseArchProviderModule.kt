package work.curioustools.ae_single.di

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BODY
import okhttp3.logging.HttpLoggingInterceptor.Level.NONE
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import work.curioustools.ae_single.BuildConfig
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Named


@Module
@InstallIn(SingletonComponent::class)
class BaseArchProviderModule {

    // ---- BASIC APP ARCHITECTURE

    companion object {
        const val APP_ALL_API_URL = "https://reqres.in/"
        const val HEADER_SKIP_ALL = "skip_all"

    }

    @Provides
    fun provideChuckInterceptor(@ApplicationContext context: Context): ChuckerInterceptor {
        return ChuckerInterceptor.Builder(context)
            .collector(ChuckerCollector(context, true, RetentionManager.Period.ONE_HOUR))
            .maxContentLength(250000L)
            .alwaysReadResponseBody(false)
            .build()
    }

    @Provides
    fun provideGson():Gson{
        return GsonBuilder().serializeNulls().create()
    }

    @Provides
    fun provideHttpLoggingInterceptor():HttpLoggingInterceptor{
        val isDebug: Boolean = BuildConfig.DEBUG
        return  HttpLoggingInterceptor().apply {
            level = if (isDebug) BODY else NONE
        }
    }

    @Provides @Named("header_interceptor")
    fun provideHeaderInterceptor():Interceptor{
        return Interceptor { chain ->
            val orig = chain.request()
            val requestBuilder =
                if (orig.header(HEADER_SKIP_ALL).equals(HEADER_SKIP_ALL)) {
                    orig.newBuilder().removeHeader(HEADER_SKIP_ALL)
                } else {
                    orig.newBuilder().apply {
                        header("Authorization", String.format("%s %s", "Bearer","use_preferences"))
                        header("Accept","application/json")
                        header("x-os", "android")
                        header("x-app-version", BuildConfig.VERSION_NAME)
                        header("x-access-cookie", "")
                        header("x-timezone-offset", Calendar.getInstance().timeZone.getOffset(Date().time).toString())
                        header("x-application-name", BuildConfig.APPLICATION_ID)
                    }
                }

            chain.proceed(requestBuilder.build())
        }
    }

    @Provides
    fun provideOkHttpClient(
        chuck: ChuckerInterceptor,
        httpLoggingInterceptor: HttpLoggingInterceptor,
        @Named("header_interceptor")  headerInterceptor: Interceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .addInterceptor(httpLoggingInterceptor)
            .addInterceptor(chuck)
            .addInterceptor(headerInterceptor)
            .connectTimeout(120L, TimeUnit.SECONDS)
            .readTimeout(120L, TimeUnit.SECONDS)
            .hostnameVerifier { _, _ -> true }
            .build()

    }


    @Provides
    fun provideRetrofit(okHttpClient: OkHttpClient ,gson: Gson): Retrofit {

        return Retrofit.Builder()
            .baseUrl(APP_ALL_API_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

}