package com.app.vv.infscan.di

import android.content.Context
import androidx.room.Room
import com.app.vv.infscan.BuildConfig
import com.app.vv.infscan.ads.AdManager
import com.app.vv.infscan.ads.GoogleAdManager
import com.app.vv.infscan.analytics.AnalyticsTracker
import com.app.vv.infscan.analytics.FirebaseAnalyticsTracker
import com.app.vv.infscan.core.ApplicationScope
import com.app.vv.infscan.core.IoDispatcher
import com.app.vv.infscan.data.db.HistoryDao
import com.app.vv.infscan.data.db.InfDatabase
import com.app.vv.infscan.data.prefs.DataStoreSettingsRepository
import com.app.vv.infscan.data.prefs.SettingsRepository
import com.app.vv.infscan.data.repo.ConnectivityNetworkMonitor
import com.app.vv.infscan.data.repo.FakePriceRepository
import com.app.vv.infscan.data.repo.HistoryRepository
import com.app.vv.infscan.data.repo.HttpLinkResolver
import com.app.vv.infscan.data.repo.LinkResolver
import com.app.vv.infscan.data.repo.NetworkMonitor
import com.app.vv.infscan.data.repo.OpenFoodFactsProductRepository
import com.app.vv.infscan.data.repo.PriceCheckFlag
import com.app.vv.infscan.data.repo.PriceRepository
import com.app.vv.infscan.data.repo.ProductRepository
import com.app.vv.infscan.data.repo.RoomHistoryRepository
import com.app.vv.infscan.platform.BarcodeImageDecoder
import com.app.vv.infscan.platform.Haptics
import com.app.vv.infscan.platform.ImageSaver
import com.app.vv.infscan.platform.MediaStoreImageSaver
import com.app.vv.infscan.platform.MlKitBarcodeImageDecoder
import com.app.vv.infscan.platform.QrImageFactory
import com.app.vv.infscan.platform.VibratorHaptics
import com.app.vv.infscan.platform.ZxingQrImageFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds abstract fun settings(impl: DataStoreSettingsRepository): SettingsRepository
    @Binds abstract fun history(impl: RoomHistoryRepository): HistoryRepository
    @Binds abstract fun network(impl: ConnectivityNetworkMonitor): NetworkMonitor
    @Binds abstract fun products(impl: OpenFoodFactsProductRepository): ProductRepository
    @Binds abstract fun prices(impl: FakePriceRepository): PriceRepository
    @Binds abstract fun links(impl: HttpLinkResolver): LinkResolver
    @Binds abstract fun images(impl: MediaStoreImageSaver): ImageSaver
    @Binds abstract fun haptics(impl: VibratorHaptics): Haptics
    @Binds abstract fun decoder(impl: MlKitBarcodeImageDecoder): BarcodeImageDecoder
    @Binds abstract fun qrImages(impl: ZxingQrImageFactory): QrImageFactory
    @Binds abstract fun analytics(impl: FirebaseAnalyticsTracker): AnalyticsTracker
    @Binds abstract fun ads(impl: GoogleAdManager): AdManager
}

@Module
@InstallIn(SingletonComponent::class)
object ProvidersModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): InfDatabase =
        Room.databaseBuilder(context, InfDatabase::class.java, "infscan.db").build()

    @Provides fun historyDao(db: InfDatabase): HistoryDao = db.historyDao()

    @Provides @IoDispatcher
    fun io(): CoroutineDispatcher = Dispatchers.IO

    @Provides @Singleton @ApplicationScope
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides @Singleton
    fun clock(): Clock = Clock.systemDefaultZone()

    @Provides @Singleton
    fun priceFlag(): PriceCheckFlag = PriceCheckFlag(BuildConfig.PRICE_CHECK_ENABLED)
}
