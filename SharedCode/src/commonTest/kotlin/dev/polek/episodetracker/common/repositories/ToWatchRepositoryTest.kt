package dev.polek.episodetracker.common.repositories

import com.russhwolf.settings.Settings
import com.russhwolf.settings.invoke
import com.squareup.sqldelight.db.SqlDriver
import dev.polek.episodetracker.common.database.createInMemorySqlDriver
import dev.polek.episodetracker.common.datasource.db.adapters.ListOfStringsAdapter
import dev.polek.episodetracker.common.datasource.omdb.OmdbService
import dev.polek.episodetracker.common.datasource.themoviedb.TmdbService
import dev.polek.episodetracker.common.network.Connectivity
import dev.polek.episodetracker.common.preferences.Preferences
import dev.polek.episodetracker.common.testutils.MockAnalytics
import dev.polek.episodetracker.common.testutils.mockTmdbHttpClient
import dev.polek.episodetracker.common.utils.parseDate
import dev.polek.episodetracker.db.Database
import dev.polek.episodetracker.db.MyShow
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore

@Ignore
class ToWatchRepositoryTest {

    private lateinit var sqlDriver: SqlDriver
    private lateinit var db: Database
    private lateinit var preferences: Preferences
    private lateinit var myShowsRepository: MyShowsRepository
    private lateinit var toWatchRepository: ToWatchRepository

    @BeforeTest
    fun setup() {
        sqlDriver = createInMemorySqlDriver()
        db = Database(
            driver = sqlDriver,
            MyShowAdapter = MyShow.Adapter(genresAdapter = ListOfStringsAdapter, networksAdapter = ListOfStringsAdapter))
        preferences = Preferences(Settings(), MockAnalytics())
        val tmdbService = TmdbService(client = mockTmdbHttpClient)
        val omdbService = OmdbService(client = mockTmdbHttpClient)
        val connectivity = object : Connectivity {
            override fun isConnected() = true
            override fun addListener(listener: Connectivity.Listener) {}
            override fun removeListener(listener: Connectivity.Listener) {}
        }

        val showRepository = ShowRepository(tmdbService, omdbService, db, preferences)
        myShowsRepository = MyShowsRepository(db, AddToMyShowsQueue(db, tmdbService, connectivity, showRepository))
        toWatchRepository = ToWatchRepository(db, preferences)
    }

    @AfterTest
    fun tearDown() {
        sqlDriver.close()
    }

    private fun String.millis(): Long = requireNotNull(parseDate(this)).timestamp

    private fun now(date: String) {
        db.timeQueries.mockCurrentTimeMillis(date.millis())
    }
}
