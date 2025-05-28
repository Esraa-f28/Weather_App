package com.example.myapplication.ui.favourites.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.example.myapplication.model.local.FavoritePlace
import com.example.myapplication.model.repo.Repository
import com.example.weatherapp.model.pojos.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

// Inline getOrAwaitValue function to avoid import issues
fun <T> LiveData<T>.getOrAwaitValue(
 time: Long = 2,
 timeUnit: TimeUnit = TimeUnit.SECONDS
): T {
 var data: T? = null
 val latch = CountDownLatch(1)
 val observer = object : Observer<T> {
  override fun onChanged(t: T) {
   data = t
   latch.countDown()
   this@getOrAwaitValue.removeObserver(this)
  }
 }
 observeForever(observer)
 try {
  if (!latch.await(time, timeUnit)) {
   throw TimeoutException("LiveData value was never set.")
  }
 } finally {
  removeObserver(observer)
 }
 @Suppress("UNCHECKED_CAST")
 return data as T
}

@RunWith(JUnit4::class)
class FavViewModelTest {

 @get:Rule
 val instantTaskExecutorRule = InstantTaskExecutorRule() // For LiveData testing

 private lateinit var viewModel: FavViewModel
 private lateinit var repository: Repository
 private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

 // Sample test data
 private val testFavoritePlace = FavoritePlace(
  latitude = 40.7128,
  longitude = -74.0060,
  name = "New York",
  city = "New York City"
 )

 private val mockCurrentWeatherResponse = CurrentWeatherResponse(
  id_current = 0,
  coord = CurrentCoord(lon = -74.0060, lat = 40.7128),
  weather = listOf(CurrentWeather(id = 800, main = "Clear", description = "clear sky", icon = "01d")),
  base = "stations",
  main = CurrentMain(
   temp = 25.0f,
   feelsLike = 24.5,
   tempMin = 23.0f,
   tempMax = 27.0f,
   pressure = 1012,
   humidity = 60,
   seaLevel = 1012,
   grndLevel = 1000
  ),
  visibility = 10000,
  wind = CurrentWind(speed = 5.5, deg = 180, gust = 7.0),
  clouds = CurrentClouds(all = 0),
  dt = 1661871600,
  sys = CurrentSys(type = 1, id = 123, country = "US", sunrise = 1661834187, sunset = 1661882248),
  timezone = -14400,
  id = 5128581,
  name = "New York",
  cod = 200
 )

 private val mockWeatherResponse = WeatherResponse(
  id = 0,
  cod = "200",
  message = 0,
  cnt = 40,
  list = listOf(
   WeatherData(
    dt = 1661871600,
    main = Main(
     temp = 25.0f,
     feelsLike = 24.5f,
     tempMin = 23.0f,
     tempMax = 27.0f,
     pressure = 1012,
     seaLevel = 1012,
     grndLevel = 1000,
     humidity = 60,
     tempKf = 0.0f
    ),
    weather = listOf(Weather(id = 800, main = "Clear", description = "clear sky", icon = "01d")),
    clouds = Clouds(all = 0),
    wind = Wind(speed = 5.5f, deg = 180, gust = 7.0f),
    visibility = 10000,
    pop = 0.0f,
    sys = Sys(pod = "d"),
    dtTxt = "2025-05-27 12:00:00"
   ),
   WeatherData(
    dt = 1661875200,
    main = Main(
     temp = 26.0f,
     feelsLike = 25.5f,
     tempMin = 24.0f,
     tempMax = 28.0f,
     pressure = 1013,
     seaLevel = 1013,
     grndLevel = 1001,
     humidity = 58,
     tempKf = 0.0f
    ),
    weather = listOf(Weather(id = 801, main = "Clouds", description = "few clouds", icon = "02d")),
    clouds = Clouds(all = 20),
    wind = Wind(speed = 6.0f, deg = 190, gust = 8.0f),
    visibility = 10000,
    pop = 0.1f,
    sys = Sys(pod = "d"),
    dtTxt = "2025-05-27 13:00:00"
   )
  ),
  city = City(
   id = 5128581,
   name = "New York",
   coord = Coord(lat = 40.7128f, lon = -74.0060f),
   country = "US",
   population = 8175133,
   timezone = -14400,
   sunrise = 1661834187,
   sunset = 1661882248
  )
 )

 @Before
 fun setup() {
  Dispatchers.setMain(testDispatcher)
  repository = mockk()
  // Mock getFavoritePlaces for init block to avoid unmocked call
  coEvery { repository.getFavoritePlaces() } returns emptyList()
  viewModel = FavViewModel(repository)
 }

 @After
 fun tearDown() {
  Dispatchers.resetMain()
 }

 @Test
 fun addFavoritePlace_adds_place_and_updates_list() = runTest {
  // Arrange
  coEvery { repository.addFavoritePlace(any()) } returns Unit
  coEvery { repository.getFavoritePlaces() } returns listOf(testFavoritePlace)

  // Act
  viewModel.addFavoritePlace(40.7128, -74.0060, "New York", "New York City")
  advanceUntilIdle()

  // Assert
  val result = viewModel.favoritePlaces.getOrAwaitValue()
  assertThat(result.size, `is`(1))
  assertThat(result[0].name, `is`("New York"))
  assertThat(result[0].city, `is`("New York City"))
  coVerify(exactly = 1) { repository.addFavoritePlace(match {
   it.latitude == 40.7128 && it.longitude == -74.0060 && it.name == "New York" && it.city == "New York City"
  }) }
  coVerify(exactly = 2) { repository.getFavoritePlaces() } // One from init, one from addFavoritePlace
 }

 @Test
 fun fetchWeatherData_emits_success_when_repository_returns_data() = runTest {
  // Arrange
  coEvery { repository.getCurrentWeather(40.7128, -74.0060, "apiKey", "metric", "en") } returns Result.success(mockCurrentWeatherResponse)
  coEvery { repository.getHourlyForecast(40.7128, -74.0060, "apiKey", "metric", "en") } returns Result.success(mockWeatherResponse)

  // Act
  viewModel.fetchWeatherData(40.7128, -74.0060, "apiKey", "metric", "en")
  advanceUntilIdle()

  // Assert
  val currentWeather = viewModel.currentWeather.getOrAwaitValue()
  assertThat(currentWeather.name, `is`("New York"))
  assertThat(currentWeather.main.temp, `is`(25.0f))
  val hourlyForecast = viewModel.hourlyForecast.getOrAwaitValue()
  assertThat(hourlyForecast.size, `is`(2)) // Mock has 2 entries
  assertThat(hourlyForecast[0].main.temp, `is`(25.0f))
  val dailyForecast = viewModel.dailyForecast.getOrAwaitValue()
  assertThat(dailyForecast.size, `is`(1)) // One day in mock data
  assertThat(dailyForecast[0].dtTxt, `is`("2025-05-27 12:00:00"))
  coVerify(exactly = 1) { repository.getCurrentWeather(40.7128, -74.0060, "apiKey", "metric", "en") }
  coVerify(exactly = 1) { repository.getHourlyForecast(40.7128, -74.0060, "apiKey", "metric", "en") }
  coVerify(exactly = 1) { repository.getFavoritePlaces() } // Only from init
 }

}