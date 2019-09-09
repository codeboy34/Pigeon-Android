package com.pigeonmessenger.di

import android.content.Context
import com.birbit.android.jobqueue.config.Configuration
import com.birbit.android.jobqueue.log.CustomLogger
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.database.room.dbs.MessageRoomDatabase
import com.pigeonmessenger.job.BaseJob
import com.pigeonmessenger.job.JobNetworkUtil
import com.pigeonmessenger.job.PigeonJobManager
import com.pigeonmessenger.manager.FirebaseTokenManager
import com.pigeonmessenger.manager.SocketManager
import com.pigeonmessenger.repo.AccountRepo
import com.pigeonmessenger.repo.ContactsRepo
import com.pigeonmessenger.repo.ConversationRepo
import com.pigeonmessenger.utils.AttachmentUtil
import com.pigeonmessenger.utils.Constant
import dagger.Module
import dagger.Provides
import org.whispersystems.libsignal.logging.Log
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import okhttp3.OkHttpClient
import com.google.gson.GsonBuilder
import com.pigeonmessenger.api.*
import com.pigeonmessenger.crypto.SignalProtocol
import com.pigeonmessenger.crypto.db.SignalDatabase
import com.pigeonmessenger.database.room.daos.*
import com.pigeonmessenger.extension.networkConnected
import com.pigeonmessenger.job.LinkState
import com.pigeonmessenger.manager.TypingManager
import com.pigeonmessenger.vo.CallState
import kotlin.math.abs


@Module
class AppModule {

    val TAG = "AppModule"

    @Provides
    fun provideContext(): Context {
        return App.get().applicationContext
    }

    @Singleton
    @Provides
    fun provideJobManager(context: Context, jobNetworkUtil: JobNetworkUtil): PigeonJobManager {
        return PigeonJobManager(Configuration.Builder(context)
                .consumerKeepAlive(20)
                .customLogger(object : CustomLogger {
                    override fun v(text: String?, vararg args: Any?) {
                        Log.v(TAG, text)
                    }

                    override fun e(t: Throwable?, text: String?, vararg args: Any?) {
                        Log.e(TAG, text, t)
                    }

                    override fun e(text: String?, vararg args: Any?) {
                        Log.e(TAG, text)
                    }

                    override fun d(text: String?, vararg args: Any?) {
                        Log.d(TAG, text)
                    }

                    override fun isDebugEnabled(): Boolean {
                        return true
                    }

                })
                .maxConsumerCount(6)
                .minConsumerCount(1)
                .networkUtil(jobNetworkUtil)
                .injector { job -> (job as? BaseJob)?.inject(App.get().appComponent) }
                .build())
    }

    @Singleton
    @Provides
    fun provideLinkState(): LinkState {
        val linkState = LinkState()
        linkState.state = LinkState.OFFLINE
        return linkState
    }

    @Singleton
    @Provides
    fun provideNetworkUtil(context: Context, linkState: LinkState): JobNetworkUtil = JobNetworkUtil(context, linkState)


    @Singleton
    @Provides
    fun provideAttachmentSessionDao(context: Context) =
            MessageRoomDatabase.getInstance(context).getAttachmentSessionDao()

    @Singleton
    @Provides
    fun provideMessageDao(context: Context): MessageDao =
            MessageRoomDatabase.getInstance(context).getMessageDao()


    @Singleton
    @Provides
    fun provideContactsDao(context: Context): ContactsDao =
            MessageRoomDatabase.getInstance(context).getContactsDao()

    @Provides
    @Singleton
    fun provideSocketManger(floodMessageDao: FloodMessageDao, messageDao:
    MessageDao, linkState: LinkState,
                            jobManager: PigeonJobManager, typingManager: TypingManager): SocketManager = SocketManager(floodMessageDao, messageDao, linkState, jobManager, typingManager)


    @Provides
    @Singleton
    fun provideFloodMessageDao(context: Context) = MessageRoomDatabase.getInstance(context).getFloodMessageDao()

    @Provides
    @Singleton
    fun providesConversationDao(context: Context) = MessageRoomDatabase.getInstance(context).getConversationDao()

    @Singleton
    @Provides
    fun provideConversionRepo(messageDao: MessageDao, conversationDao: ConversationDao
                              , participantDao: ParticipantDao, roomDatabase: MessageRoomDatabase):
            ConversationRepo = ConversationRepo(messageDao, conversationDao, participantDao, roomDatabase)

    @Provides
    fun provideMessageDatabase(context: Context) = MessageRoomDatabase.getInstance(context)

    @Singleton
    @Provides
    fun provideAttachmentUtil(): AttachmentUtil = AttachmentUtil()


    @Singleton
    @Provides
    fun provideRetrofit(): Retrofit {
        val gson = GsonBuilder().setLenient().create()
        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            val token = FirebaseTokenManager.getInstance().token
            val request = requestBuilder.addHeader("idtoken", token).build()

            if (App.get().networkConnected()) {
                val response = try {
                    chain.proceed(request)
                } catch (e: Exception) {
                    if (e.message?.contains("502") == true) {
                        throw ServerErrorException(502)
                    } else throw e
                }

                if (App.get().onlining.get()) {
                    response.header("X-Server-Time")?.toLong()?.let { serverTime ->
                        if (abs(serverTime / 1000000 - System.currentTimeMillis()) >= 600000L) {
                            App.get().gotoTimeWrong(serverTime)
                        }
                    }
                }

                if (!response.isSuccessful) {
                    val code = response.code()
                    if (code in 500..599) {
                        throw ServerErrorException(code)
                    }
                }
                return@addInterceptor response
            } else {
                throw NetworkException()
            }

            //chain.proceed(request)
        }

        /*
        val sslContext = SSLContext.getInstance("SSL")
        val trustAllCerts = arrayOf<TrustManager>(object : TrustManager {
            val acceptedIssuers: Array<java.security.cert.X509Certificate>? get() = null

            @Throws(CertificateException::class)
            fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
            }

            @Throws(CertificateException::class)
            fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
            }
        })

        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(null as KeyStore?)

        var x509Tm: X509TrustManager? = null
        for (tm in tmf.trustManagers) {
            if (tm is X509TrustManager) {
                x509Tm = tm
                break
            }
        }
        val finalTm = x509Tm
        val x509TrustManager = object : X509TrustManager{
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                finalTm?.checkClientTrusted(chain, authType)
            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                finalTm?.checkServerTrusted(chain, authType);
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return finalTm!!.acceptedIssuers;
            }

        }


        //sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        // Create an ssl socket factory with our all-trusting manager
        val sslSocketFactory = sslContext.socketFactory
        //httpClient.sslSocketFactory(sslSocketFactory, x509TrustManager)
*/
       /* httpClient.hostnameVerifier(object : HostnameVerifier {
            override fun verify(hostname: String, session: SSLSession): Boolean {
                return true
            }
        })*/
       // httpClient.connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
        return Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addConverterFactory(NullOnEmptyConverterFactory())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(httpClient.build())
                .baseUrl(Constant.URL).build()
    }

    @Singleton
    @Provides
    fun provideContactsService(retrofit: Retrofit): ContactsService = retrofit.create(ContactsService::class.java)

    @Singleton
    @Provides
    fun provideAccountService(retrofit: Retrofit): AccountService = retrofit.create(AccountService::class.java)

    @Singleton
    @Provides
    fun provideAccountRepo(accountService: AccountService) = AccountRepo(accountService)

    @Singleton
    @Provides
    fun provideContactsRepo(contactsDao: ContactsDao) = ContactsRepo(contactsDao)

    @Singleton
    @Provides
    fun provideHyperlinkDao(context: Context) = MessageRoomDatabase.getInstance(context).hyperlinkDao()


    @Singleton
    @Provides
    fun provideCallState(context: Context) = CallState()

    @Singleton
    @Provides
    fun participantDao(roomDatabase: MessageRoomDatabase) = roomDatabase.participantDao()

    @Singleton
    @Provides
    fun conversationService(retrofit: Retrofit): ConversationService = retrofit.create(ConversationService::class.java)

    @Singleton
    @Provides
    fun signalProtocol(context: Context) = SignalProtocol(context)

    @Singleton
    @Provides
    fun signalService(retrofit: Retrofit): SignalKeyService = retrofit.create(SignalKeyService::class.java)

    @Singleton
    @Provides
    fun senderKeyDao(context: Context) = SignalDatabase.getDatabase(context).senderKeyDao()

    @Singleton
    @Provides
    fun ratchetSenderKeyDao(context: Context) = SignalDatabase.getDatabase(context).ratchetSenderKeyDao()


    @Singleton
    @Provides
    fun sentSenderKeyDap(roomDatabase: MessageRoomDatabase) = roomDatabase.sentSenderKeyDao()

    @Singleton
    @Provides
    fun resendMessageDao(messageRoomDatabase: MessageRoomDatabase) = messageRoomDatabase.resendMessageDao()

    @Singleton
    @Provides
    fun typingManager() = TypingManager()

    @Singleton
    @Provides
    fun settingsService(retrofit: Retrofit): SettingsService = retrofit.create(SettingsService::class.java)


}