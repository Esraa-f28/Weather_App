package com.example.myapplication.model.repo

import com.example.myapplication.model.local.AlarmType
import com.example.myapplication.model.local.Alert
import com.example.myapplication.model.local.FakeLocalDataSource
import com.example.myapplication.model.local.FavoritePlace
import com.example.myapplication.model.remote.FakeRemoteDataSource
import com.example.weatherapp.model.pojos.City
import com.example.weatherapp.model.pojos.Clouds
import com.example.weatherapp.model.pojos.Coord
import com.example.weatherapp.model.pojos.CurrentClouds
import com.example.weatherapp.model.pojos.CurrentCoord
import com.example.weatherapp.model.pojos.CurrentMain
import com.example.weatherapp.model.pojos.CurrentSys
import com.example.weatherapp.model.pojos.CurrentWeather
import com.example.weatherapp.model.pojos.CurrentWeatherResponse
import com.example.weatherapp.model.pojos.CurrentWind
import com.example.weatherapp.model.pojos.Main
import com.example.weatherapp.model.pojos.Sys
import com.example.weatherapp.model.pojos.Weather
import com.example.weatherapp.model.pojos.WeatherData
import com.example.weatherapp.model.pojos.WeatherResponse
import com.example.weatherapp.model.pojos.Wind
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Test

class WeatherRepositoryTest {
 lateinit var localDataSource: FakeLocalDataSource
 lateinit var remoteDataSource: FakeRemoteDataSource
 lateinit var repo: Repository

 private val mockHourlyForecastResponse = WeatherResponse(
  id = 0,
  cod = "200",
  message = 0,
  cnt = 1,
  list = listOf(
   WeatherData(
    dt = 1627040400L,
    main = Main(
     temp = 298.55f,
     feelsLike = 298.95f,
     tempMin = 298.55f,
     tempMax = 298.56f,
     pressure = 1013,
     seaLevel = 1013,
     grndLevel = 1005,
     humidity = 87,
     tempKf = 0.01f
    ),
    weather = listOf(
     Weather(
      id = 500,
      main = "Rain",
      description = "light rain",
      icon = "10d"
     )
    ),
    clouds = Clouds(all = 75),
    wind = Wind(speed = 4.12f, deg = 240, gust = 7.2f),
    visibility = 10000,
    pop = 0.2f,
    sys = Sys(pod = "d"),
    dtTxt = "2021-07-23 15:00:00"
   )
  ),
  city = City(
   id = 2643743,
   name = "London",
   coord = Coord(lat = 51.5085f, lon = -0.1257f),
   country = "GB",
   population = 1000000,
   timezone = 3600,
   sunrise = 1627014875L,
   sunset = 1627071392L
  )
 )

 private val mockCurrentWeatherResponse = CurrentWeatherResponse(
  id_current = 0,
  coord = CurrentCoord(
   lon = -0.1257,
   lat = 51.5085
  ),
  weather = listOf(
   CurrentWeather(
    id = 800,
    main = "Clear",
    description = "clear sky",
    icon = "01d"
   )
  ),
  base = "stations",
  main = CurrentMain(
   temp = 293.15f,
   feelsLike = 292.86,
   tempMin = 292.15f,
   tempMax = 294.15f,
   pressure = 1012,
   humidity = 60,
   seaLevel = 1012,
   grndLevel = 1008
  ),
  visibility = 10000,
  wind = CurrentWind(
   speed = 3.6,
   deg = 200,
   gust = 0.0
  ),
  clouds = CurrentClouds(
   all = 0
  ),
  dt = 1627040400L,
  sys = CurrentSys(
   type = 1,
   id = 1414,
   country = "GB",
   sunrise = 1627014875L,
   sunset = 1627071392L
  ),
  timezone = 3600,
  id = 2643743,
  name = "London",
  cod = 200
 )

 private val mockFavoritePlace = FavoritePlace(
  id = 0,
  latitude = 35.6895,
  longitude = 139.6917,
  name = "Tokyo",
  city = "Tokyo"
 )

 private val mockAlert = Alert(
  id = "1",
  durationHours = 1,
  alarmType = AlarmType.DEFAULT_ALARM,
  isActive = true,
  fromTimeMillis = 1727452800000L,
  toTimeMillis = 1727456400000L
 )

 @Before
 fun setup() {
  val weatherResponses = mutableMapOf(Pair(51.5085, -0.1257) to mockHourlyForecastResponse)
  val currentWeatherResponses = mutableMapOf(Pair(51.5085, -0.1257) to mockCurrentWeatherResponse)
  localDataSource = FakeLocalDataSource(
   weatherResponses = weatherResponses,
   currentWeatherResponses = currentWeatherResponses,
   favoritePlaces = mutableListOf(),
   alerts = mutableListOf()
  )
  remoteDataSource = FakeRemoteDataSource(
   weatherResponses = weatherResponses,
   currentWeatherResponses = currentWeatherResponses
  )
  repo = Repository(remoteDataSource, localDataSource)
 }

 @Test
 fun getCurrentWeather_validCoordinates_returnsSuccess() = runTest {
  val result = repo.getCurrentWeather(
   latitude = 51.5085,
   longitude = -0.1257,
   apiKey = "valid_api_key"
  )
  assertThat(result.isSuccess, `is`(true))
  result.onSuccess { response ->
   assertThat(response.name, `is`("London"))
   assertThat(response.main.temp, `is`(293.15f))
  }
 }

 @Test
 fun getHourlyForecast_validCoordinates_returnsSuccess() = runTest {
  val result = repo.getHourlyForecast(
   latitude = 51.5085,
   longitude = -0.1257,
   apiKey = "valid_api_key"
  )
  assertThat(result.isSuccess, `is`(true))
  result.onSuccess { response ->
   assertThat(response.city.name, `is`("London"))
   assertThat(response.list[0].main.temp, `is`(298.55f))
  }
 }

 @Test
 fun saveHourlyForecastToLocal_savesCorrectly() = runTest {
  repo.saveHourlyWeatherResponse(mockHourlyForecastResponse)
  val retrieved = localDataSource.getCachedHourlyWeatherResponse(0)
  assertThat(retrieved?.city?.name, `is`("London"))
 }

 @Test
 fun saveCurrentWeatherToLocal_savesCorrectly() = runTest {
  repo.saveCurrentWeatherResponse(mockCurrentWeatherResponse)
  val retrieved = localDataSource.getCachedCurrentWeatherResponse(0)
  assertThat(retrieved?.name, `is`("London"))
 }

 @Test
 fun favoritePlaceOperations_workCorrectly() = runTest {
  // Test add
  repo.addFavoritePlace(mockFavoritePlace)
  val locations = repo.getFavoritePlaces()
  assertThat(locations.size, `is`(1))
  assertThat(locations[0].name, `is`("Tokyo"))

  // Test delete
  repo.deleteFavoritePlace(mockFavoritePlace)
  val updatedLocations = repo.getFavoritePlaces()
  assertThat(updatedLocations.size, `is`(0))
 }

 @Test
 fun alertOperations_workCorrectly() = runTest {
  // Test add
  repo.addAlert(mockAlert)
  val alerts = repo.getAlerts()
  assertThat(alerts.size, `is`(1))
  assertThat(alerts[0].id, `is`("1"))

  // Test update status
  repo.updateAlertStatus(mockAlert.id, false)
  val updatedAlerts = repo.getAlerts()
  assertThat(updatedAlerts[0].isActive, `is`(false))

  // Test delete
  repo.deleteAlert(mockAlert.id)
  val finalAlerts = repo.getAlerts()
  assertThat(finalAlerts.size, `is`(0))
 }
}